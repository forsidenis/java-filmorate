-- Таблица рейтингов MPA
CREATE TABLE IF NOT EXISTS mpa_ratings (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Таблица жанров
CREATE TABLE IF NOT EXISTS genres (
    id INTEGER PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

-- Таблица фильмов
CREATE TABLE IF NOT EXISTS films (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(200),
    release_date DATE NOT NULL,
    duration INTEGER NOT NULL,
    mpa_id INTEGER NOT NULL,
    CONSTRAINT fk_films_mpa FOREIGN KEY (mpa_id) REFERENCES mpa_ratings(id) ON DELETE RESTRICT
);

-- Связь фильмов и жанров (многие-ко-многим)
CREATE TABLE IF NOT EXISTS film_genres (
    film_id INTEGER NOT NULL,
    genre_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, genre_id),
    CONSTRAINT fk_film_genres_films FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    CONSTRAINT fk_film_genres_genres FOREIGN KEY (genre_id) REFERENCES genres(id) ON DELETE CASCADE
);

-- Таблица пользователей
CREATE TABLE IF NOT EXISTS users (
    id INTEGER AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    login VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255),
    birthday DATE NOT NULL
);

-- Таблица лайков фильмов
CREATE TABLE IF NOT EXISTS film_likes (
    film_id INTEGER NOT NULL,
    user_id INTEGER NOT NULL,
    PRIMARY KEY (film_id, user_id),
    CONSTRAINT fk_film_likes_films FOREIGN KEY (film_id) REFERENCES films(id) ON DELETE CASCADE,
    CONSTRAINT fk_film_likes_users FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Таблица друзей (односторонняя дружба)
CREATE TABLE IF NOT EXISTS friends (
    user_id INTEGER NOT NULL,
    friend_id INTEGER NOT NULL,
    confirmed BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (user_id, friend_id),
    CONSTRAINT fk_friends_users_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_friends_users_friend FOREIGN KEY (friend_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Индексы для улучшения производительности

-- Индексы для таблицы films
CREATE INDEX IF NOT EXISTS idx_films_mpa_id ON films(mpa_id);
CREATE INDEX IF NOT EXISTS idx_films_release_date ON films(release_date);

-- Индексы для таблицы film_genres
CREATE INDEX IF NOT EXISTS idx_film_genres_film_id ON film_genres(film_id);
CREATE INDEX IF NOT EXISTS idx_film_genres_genre_id ON film_genres(genre_id);

-- Индексы для таблицы film_likes
CREATE INDEX IF NOT EXISTS idx_film_likes_film_id ON film_likes(film_id);
CREATE INDEX IF NOT EXISTS idx_film_likes_user_id ON film_likes(user_id);

-- Индексы для таблицы friends
CREATE INDEX IF NOT EXISTS idx_friends_user_id ON friends(user_id);
CREATE INDEX IF NOT EXISTS idx_friends_friend_id ON friends(friend_id);

-- Индексы для таблицы users
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_login ON users(login);
