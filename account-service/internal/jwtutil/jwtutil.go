package jwtutil

import (
	"errors"
	"fmt"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

// CustomClaims - полезная нагрузка
type CustomClaims struct {
	Role      string `json:"role"`
	Email     string `json:"email"`
	TokenType string `json:"token_type"`
	UserID    int64  `json:"user_id"`
	jwt.RegisteredClaims
}

// JWTValidator - хранит секрет (как массив байт) для проверки подписи
type JWTValidator struct {
	secret []byte
}

// New - создаёт валидатор из секрета
func New(secret string) *JWTValidator {
	return &JWTValidator{secret: []byte(secret)}
}

// Validate - парсит токен и проверяет подпись и срок истечения
func (v *JWTValidator) Validate(tokenStr string) (*CustomClaims, error) {
	if tokenStr == "" {
		return nil, errors.New("token is empty")
	}

	token, err := jwt.ParseWithClaims(tokenStr, &CustomClaims{}, func(token *jwt.Token) (interface{}, error) {
		// ожидаем HMAC
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return v.secret, nil
	},
		jwt.WithLeeway(5*time.Second),
	)
	if err != nil {
		return nil, err
	}

	claims, ok := token.Claims.(*CustomClaims)
	if !ok || !token.Valid {
		return nil, errors.New("invalid token")
	}

	return claims, nil
}
