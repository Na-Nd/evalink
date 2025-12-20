package jwtutil

import (
	"errors"
	"fmt"
	"log"
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

	// Парсим и валидируем (с небольшим leeway).
	token, err := jwt.ParseWithClaims(tokenStr, &CustomClaims{}, func(token *jwt.Token) (interface{}, error) {
		// ожидаем HMAC
		if _, ok := token.Method.(*jwt.SigningMethodHMAC); !ok {
			return nil, fmt.Errorf("unexpected signing method: %v", token.Header["alg"])
		}
		return v.secret, nil
	}, jwt.WithLeeway(30*time.Second)) // <-- увеличим leeway для межсервисных токенов, при необходимости уменьшите

	if err != nil {
		// Если токен просрочен — попробуем "без валидации claims" распарсить и залогировать exp
		if errors.Is(err, jwt.ErrTokenExpired) {
			var c CustomClaims
			// Парсим без проверки claims, чтобы извлечь Expiry для логирования
			_, perr := jwt.ParseWithClaims(tokenStr, &c, func(t *jwt.Token) (interface{}, error) {
				return v.secret, nil
			}, jwt.WithoutClaimsValidation())
			if perr == nil {
				if c.ExpiresAt != nil {
					log.Printf("token expired: exp=%v, now=%v, remaining=%v", c.ExpiresAt.Time, time.Now(), time.Until(c.ExpiresAt.Time))
				} else {
					log.Printf("token expired but no Exp claim present")
				}
			} else {
				// Не получилось распарсить без валидации — логируем причину
				log.Printf("token expired; fallback parse failed: %v", perr)
			}

			return nil, fmt.Errorf("token is expired")
		}

		// Другие ошибки парсинга/валидации — логируем и возвращаем
		log.Printf("token parse/validate error: %v", err)
		return nil, err
	}

	claims, ok := token.Claims.(*CustomClaims)
	if !ok || !token.Valid {
		return nil, errors.New("invalid token")
	}

	// Успешный разбор — логируем expiry для информации
	if claims.ExpiresAt != nil {
		log.Printf("token valid: exp=%v, now=%v, remaining=%v", claims.ExpiresAt.Time, time.Now(), time.Until(claims.ExpiresAt.Time))
	}

	return claims, nil
}
