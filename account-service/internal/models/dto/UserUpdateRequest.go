package dto

// UserUpdateRequest - для обновление сущности пользователя (рассылается остальным сервисам)
type UserUpdateRequest struct {
	Username  string `json:"username"`
	Email     string `json:"email"`
	Password  string `json:"password"`
	EventType string `json:"event_type"` // "CREATE" или "UPDATE"
}
