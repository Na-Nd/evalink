package handlers

import (
	"account-service/internal/middleware"
	"account-service/internal/models/dto"
	"account-service/internal/store"
	"encoding/json"
	"net/http"
	"strconv"

	"github.com/gorilla/mux"
)

// AccountHandler — структура для хранения зависимости репозитория
type AccountHandler struct {
	store *store.Store
}

func NewAccountHandler(s *store.Store) *AccountHandler {
	return &AccountHandler{store: s}
}

// CreateAccount — принимает HTTP запрос от auth-service
func (h *AccountHandler) CreateAccount(w http.ResponseWriter, r *http.Request) {
	var regDto dto.RegisterDto

	if err := json.NewDecoder(r.Body).Decode(&regDto); err != nil {
		http.Error(w, "bad request: "+err.Error(), http.StatusBadRequest)
		return
	}

	if regDto.Username == "" || regDto.Email == "" || regDto.Password == "" {
		http.Error(w, "username, email and password required", http.StatusBadRequest)
		return
	}

	id, err := h.store.CreateUser(r.Context(), &regDto)
	if err != nil {
		http.Error(w, "failed to create user: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusCreated)
	_ = json.NewEncoder(w).Encode(map[string]interface{}{
		"message":  "user created successfully",
		"id":       id,
		"username": regDto.Username,
		"email":    regDto.Email,
	})
}

// ListAccounts - получение списка пользовательских аккаунтов
// TODO пагинация
func (h *AccountHandler) ListAccounts(w http.ResponseWriter, r *http.Request) {
	users, err := h.store.ListUsers(r.Context())
	if err != nil {
		http.Error(w, "failed to list users: "+err.Error(), http.StatusInternalServerError)
		return
	}
	_ = json.NewEncoder(w).Encode(users)
}

// GetAccount - получение конкретного аккаунта
func (h *AccountHandler) GetAccount(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, _ := strconv.ParseInt(idStr, 10, 64)

	u, err := h.store.GetUserByID(r.Context(), id)
	if err != nil {
		http.Error(w, "failed to find user: "+err.Error(), http.StatusNotFound)
		return
	}

	_ = json.NewEncoder(w).Encode(u)
}

// UpdateAccount - обновление аккаунта владельца
func (h *AccountHandler) UpdateAccount(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, _ := strconv.ParseInt(idStr, 10, 64)

	// Получаем claims, выставленные UserMiddleware
	claims := middleware.GetClaimsFromContext(r)
	if claims == nil {
		http.Error(w, "missing token claims", http.StatusUnauthorized)
		return
	}

	// Проверяем, что пользователь редактирует только свой аккаунт
	if claims.UserID != id {
		http.Error(w, "forbidden: you can update only your own account", http.StatusForbidden)
		return
	}

	var updateDto dto.UserUpdateRequest
	if err := json.NewDecoder(r.Body).Decode(&updateDto); err != nil {
		http.Error(w, "bad request: "+err.Error(), http.StatusBadRequest)
		return
	}

	if updateDto.Username == "" || updateDto.Email == "" {
		http.Error(w, "username and email required", http.StatusBadRequest)
		return
	}

	if err := h.store.UpdateUser(r.Context(), &updateDto, id); err != nil {
		http.Error(w, "update failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusOK)
	_ = json.NewEncoder(w).Encode(map[string]any{
		"message": "user updated successfully",
	})
}

// DeleteAccount - удаление аккаунта владельца
func (h *AccountHandler) DeleteAccount(w http.ResponseWriter, r *http.Request) {
	vars := mux.Vars(r)
	idStr := vars["id"]
	id, _ := strconv.ParseInt(idStr, 10, 64)

	claims := middleware.GetClaimsFromContext(r)
	if claims == nil {
		http.Error(w, "missing token claims", http.StatusUnauthorized)
		return
	}

	if claims.UserID != id {
		http.Error(w, "forbidden: you can delete only your own account", http.StatusForbidden)
		return
	}

	if err := h.store.DeleteUser(r.Context(), id); err != nil {
		http.Error(w, "delete failed: "+err.Error(), http.StatusInternalServerError)
		return
	}

	w.WriteHeader(http.StatusNoContent)
}
