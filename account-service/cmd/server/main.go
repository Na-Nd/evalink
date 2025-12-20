package main

import (
	"account-service/internal/kafka"
	"account-service/internal/schema"
	"context"
	"database/sql"
	"fmt"
	"log"
	"net/http"
	"os"
	"strings"
	"time"

	"account-service/internal/handlers"
	"account-service/internal/middleware"
	"account-service/internal/store"

	"github.com/gorilla/mux"
	"github.com/jmoiron/sqlx"
	"github.com/joho/godotenv"
	_ "github.com/lib/pq"
)

func main() {
	_ = godotenv.Load()

	dbURL := os.Getenv("DATABASE_URL")
	if dbURL == "" {
		log.Fatal("DATABASE_URL is required")
	}

	db, err := sqlx.Connect("postgres", dbURL)
	if err != nil {
		log.Fatalf("failed to connect db: %v", err)
	}
	defer db.Close()

	// Инициализация схемы
	if err := ensureSchema(db.DB, schema.SQL); err != nil {
		log.Fatalf("schema init failed: %v", err)
	}
	log.Println("Схема инициализирована")

	st := store.NewStore(db)

	// Инииализация продюсера
	var producer *kafka.Producer
	kafkaBrokers := os.Getenv("KAFKA_BROKERS")
	kafkaTopic := envOrDefault("KAFKA_TOPIC", "Account-Events-Topic")
	if strings.TrimSpace(kafkaBrokers) != "" {
		cfg := kafka.Config{
			Brokers:           kafka.ParseBrokers(kafkaBrokers),
			Topic:             kafkaTopic,
			NumPartitions:     3,
			ReplicationFactor: 3,
		}
		ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		defer cancel()
		p, err := kafka.NewProducer(ctx, cfg)
		if err != nil {
			log.Fatalf("failed to create kafka producer: %v", err)
		}
		producer = p
		// Закроем продюсер при выходе
		defer func() {
			_ = producer.Close()
		}()
	} else {
		log.Println("KAFKA_BROKERS is empty — skipping kafka producer init")
	}

	h := handlers.NewAccountHandler(st, producer)

	r := mux.NewRouter()

	// Создаём общий AuthMiddleware с двумя секретами из env
	am := middleware.NewAuthMiddleware(os.Getenv("JWT_USER_SECRET"), os.Getenv("JWT_SERVICE_SECRET"))

	api := r.PathPrefix("/api").Subrouter()

	// создание аккаунта только для auth-service
	api.Handle("/account", am.ServiceMiddleware(http.HandlerFunc(h.CreateAccount))).Methods("POST")

	// Остальные эндпоинты доступны пользователям
	api.Handle("/account", am.UserMiddleware(http.HandlerFunc(h.ListAccounts))).Methods("GET")
	api.Handle("/account/{id:[0-9]+}", am.UserMiddleware(http.HandlerFunc(h.GetAccount))).Methods("GET")
	api.Handle("/account/{id:[0-9]+}", am.UserMiddleware(http.HandlerFunc(h.UpdateAccount))).Methods("PUT")
	api.Handle("/account/{id:[0-9]+}", am.UserMiddleware(http.HandlerFunc(h.DeleteAccount))).Methods("DELETE")

	addr := fmt.Sprintf(":%s", envOrDefault("PORT", "8081"))
	log.Printf("server starting on %s", addr)
	log.Fatal(http.ListenAndServe(addr, r))
}

// envOrDefault - вспомогательная функция для определения env
func envOrDefault(key, def string) string {
	v := os.Getenv(key)

	if v == "" {
		return def
	}

	return v
}

func ensureSchema(db *sql.DB, schemaSQL string) error {
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer cancel()

	if strings.TrimSpace(schemaSQL) == "" {
		return fmt.Errorf("schema SQL is empty")
	}

	// разбиваем на выражения ";" и выполняем последовательно
	parts := strings.Split(schemaSQL, ";")
	for _, p := range parts {
		stmt := strings.TrimSpace(p)
		if stmt == "" {
			continue
		}
		if _, err := db.ExecContext(ctx, stmt); err != nil {
			return fmt.Errorf("failed to execute statement %q: %w", stmt, err)
		}
	}

	return nil
}
