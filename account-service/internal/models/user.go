package models

import "time"

type User struct {
	ID               int64     `db:"id" json:"id"`
	Username         string    `db:"username" json:"username"`
	Email            string    `db:"email" json:"email"`
	Password         string    `db:"password" json:"password"`
	Role             Role      `db:"role" json:"role"`
	IsBlocked        bool      `db:"is_blocked" json:"is_blocked"`
	RegistrationDate time.Time `db:"registration_date" json:"registration_date"`
}
