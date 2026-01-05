-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    email TEXT NOT NULL UNIQUE
);

-- Таблица постов
CREATE TABLE IF NOT EXISTS posts(
    id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    text TEXT,
    tags JSONB, -- будет как массив
    date_of_publication TIMESTAMP WITH TIME ZONE DEFAULT now(),
    date_of_update TIMESTAMP WITH TIME ZONE DEFAULT now()
);

-- Таблица изображений постов
CREATE TABLE IF NOT EXISTS post_images (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    filename TEXT NOT NULL
);

-- Таблица комментариев
CREATE TABLE IF NOT EXISTS comments (
    id BIGSERIAL PRIMARY KEY,
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE, -- При удалении аккаунта пользователя пусть чистятся его комменты
    text TEXT NOT NULL,
    date_of_creation TIMESTAMP WITH TIME ZONE DEFAULT now(),
    date_of_editing TIMESTAMP WITH TIME ZONE
);

-- Таблица лайков постов
CREATE TABLE IF NOT EXISTS post_likes (
    post_id BIGINT NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, user_id)
);