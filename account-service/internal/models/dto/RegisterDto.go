package dto

// RegisterDto - Приходит от Auth-Service по HTTP
type RegisterDto struct {
	Username      string `json:"username"`
	Email         string `json:"email"`
	Password      string `json:"password"`
	RequestId     string `json:"requestId"`
	EmailVerified bool   `json:"emailVerified"`
}
