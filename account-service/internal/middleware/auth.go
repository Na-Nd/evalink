package middleware

import (
	"context"
	"net/http"
	"strings"

	"account-service/internal/jwtutil"
)

type contextKey string

const ClaimsCtxKey contextKey = "jwt_claims"

// AuthMiddleware - структура с завимостями валидаторов межсервисного и пользовательского JWT
type AuthMiddleware struct {
	userValidator    *jwtutil.JWTValidator
	serviceValidator *jwtutil.JWTValidator
}

// NewAuthMiddleware - создаёт новый фильтр с пользовательским и сервисным секретом
func NewAuthMiddleware(userSecret, serviceSecret string) *AuthMiddleware {
	return &AuthMiddleware{
		userValidator:    jwtutil.New(userSecret),
		serviceValidator: jwtutil.New(serviceSecret),
	}
}

// ServiceMiddleware — допускает только сервисный токен (token_type == "service_token" && role == "service").
func (am *AuthMiddleware) ServiceMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		token, ok := extractBearer(r)
		if !ok {
			http.Error(w, "missing or invalid Authorization header", http.StatusUnauthorized)
			return
		}

		claims, err := am.serviceValidator.Validate(token)
		if err != nil {
			http.Error(w, "invalid service token: "+err.Error(), http.StatusUnauthorized)
			return
		}

		if claims.TokenType != "service_token" || claims.Role != "service" {
			http.Error(w, "forbidden: token is not service token", http.StatusUnauthorized)
			return
		}

		ctx := context.WithValue(r.Context(), ClaimsCtxKey, claims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// UserMiddleware — допускает только пользовательский access token (token_type == "access").
func (am *AuthMiddleware) UserMiddleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		token, ok := extractBearer(r)
		if !ok {
			http.Error(w, "missing or invalid Authorization header", http.StatusUnauthorized)
			return
		}

		claims, err := am.userValidator.Validate(token)
		if err != nil {
			http.Error(w, "invalid user token: "+err.Error(), http.StatusUnauthorized)
			return
		}

		if claims.TokenType != "access" {
			http.Error(w, "forbidden: token is not access token", http.StatusUnauthorized)
			return
		}

		ctx := context.WithValue(r.Context(), ClaimsCtxKey, claims)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

// GetClaimsFromContext - возвращает полезную нагрузку
func GetClaimsFromContext(r *http.Request) *jwtutil.CustomClaims {
	if v := r.Context().Value(ClaimsCtxKey); v != nil {
		if c, ok := v.(*jwtutil.CustomClaims); ok {
			return c
		}
	}
	return nil
}

// extractBearer - отсечение Bearer_
func extractBearer(r *http.Request) (string, bool) {
	auth := r.Header.Get("Authorization")
	if auth == "" || !strings.HasPrefix(auth, "Bearer ") {
		return "", false
	}
	return strings.TrimPrefix(auth, "Bearer "), true
}
