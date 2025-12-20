package store

import (
	"account-service/internal/models"
	"account-service/internal/models/dto"
	"context"
	"fmt"
	"time"

	"github.com/jmoiron/sqlx"
	"golang.org/x/crypto/bcrypt"
)

type Store struct {
	db *sqlx.DB
}

func NewStore(db *sqlx.DB) *Store {
	return &Store{db: db}
}

// CreateUser - создание сущности пользователя
func (s *Store) CreateUser(ctx context.Context, ru *dto.RegisterDto) (int64, error) {
	// Хэширование
	hashed, err := hashPassword(ru.Password)
	if err != nil {
		return 0, fmt.Errorf("failed to hash password: %w", err)
	}

	var id int64
	query := `
        INSERT INTO users (username, email, password, role, is_blocked, registration_date)
        VALUES ($1, $2, $3, $4, $5, $6)
        RETURNING id
    `
	err = s.db.QueryRowxContext(ctx, query,
		ru.Username,
		ru.Email,
		hashed,
		models.RoleUser,
		false,
		time.Now(),
	).Scan(&id)

	if err != nil {
		return 0, fmt.Errorf("create user: %w", err)
	}
	return id, nil
}

// ListUsers - получение списка аккаунтов
// TODO пагинация
func (s *Store) ListUsers(ctx context.Context) ([]models.User, error) {
	var users []models.User

	if err := s.db.SelectContext(ctx, &users, "SELECT id, username, email, role FROM users ORDER BY id"); err != nil {
		return nil, err
	}

	return users, nil
}

// GetUserByID - получение пользователя по id, который достается из Access
func (s *Store) GetUserByID(ctx context.Context, id int64) (*models.User, error) {
	var u models.User

	err := s.db.GetContext(ctx, &u, "SELECT id, username, email, role FROM users WHERE id = $1", id)
	if err != nil {
		return nil, err
	}

	return &u, nil
}

// UpdateUser - полное обновление сущности
func (s *Store) UpdateUser(ctx context.Context, ur *dto.UserUpdateRequest, id int64) error {
	if ur.Password == "" {
		// Обновляем только username и email
		res, err := s.db.ExecContext(ctx, "UPDATE users SET username = $1, email = $2 WHERE id = $3", ur.Username, ur.Email, id)
		if err != nil {
			return err
		}
		n, err := res.RowsAffected()
		if err != nil {
			return err
		}
		if n == 0 {
			return fmt.Errorf("user not found")
		}
		return nil
	}

	// Нужно обновить и пароль — хэшируем
	hashed, err := hashPassword(ur.Password)
	if err != nil {
		return fmt.Errorf("hash password: %w", err)
	}

	res, err := s.db.ExecContext(ctx, "UPDATE users SET username = $1, email = $2, password = $3 WHERE id = $4", ur.Username, ur.Email, hashed, id)
	if err != nil {
		return err
	}

	n, err := res.RowsAffected()
	if err != nil {
		return err
	}
	if n == 0 {
		return fmt.Errorf("user not found")
	}
	return nil
}

// DeleteUser - удаление по переданному id
func (s *Store) DeleteUser(ctx context.Context, id int64) error {
	res, err := s.db.ExecContext(ctx, "DELETE FROM users WHERE id = $1", id)
	if err != nil {
		return err
	}

	n, err := res.RowsAffected()
	if err != nil {
		return err
	}

	if n == 0 {
		return fmt.Errorf("user not found")
	}

	return nil
}

// hashPassword - вспомогательная функция для хэширования
func hashPassword(raw string) (string, error) {
	if raw == "" {
		return "", fmt.Errorf("password is empty")
	}
	hashedBytes, err := bcrypt.GenerateFromPassword([]byte(raw), bcrypt.DefaultCost)
	if err != nil {
		return "", err
	}

	return string(hashedBytes), nil
}

// ComparePassword - сравнение с захэшированным паролем
func ComparePassword(hashed, raw string) error {
	return bcrypt.CompareHashAndPassword([]byte(hashed), []byte(raw))
}
