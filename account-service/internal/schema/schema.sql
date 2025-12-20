CREATE TABLE IF NOT EXISTS users (
                                     id SERIAL PRIMARY KEY,
                                     username TEXT UNIQUE NOT NULL,
                                     email TEXT UNIQUE NOT NULL,
                                     password TEXT NOT NULL,
                                     role TEXT NOT NULL,
                                     is_blocked BOOLEAN DEFAULT FALSE,
                                     registration_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
