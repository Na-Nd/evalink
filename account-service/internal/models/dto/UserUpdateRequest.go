package dto

// UserUpdateRequest - для обновление сущности пользователя
type UserUpdateRequest struct {
	Username string `json:"username"`
	Email    string `json:"email"`
	Password string `json:"password"`
}
