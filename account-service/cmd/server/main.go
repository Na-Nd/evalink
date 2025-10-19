package main

import (
	"fmt"
	"log"
	"net/http"
	"os"

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

	st := store.NewStore(db)
	h := handlers.NewAccountHandler(st)

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

func envOrDefault(key, def string) string {
	v := os.Getenv(key)
	
	if v == "" {
		return def
	}

	return v
}
