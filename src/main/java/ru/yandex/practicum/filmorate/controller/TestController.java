package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/reset")
    public String resetDatabase() {
        log.info("=== RESETTING DATABASE ===");

        try {
            // Отключаем foreign key constraints
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

            // Очищаем таблицы в правильном порядке
            jdbcTemplate.update("DELETE FROM film_likes");
            jdbcTemplate.update("DELETE FROM film_genres");
            jdbcTemplate.update("DELETE FROM friendships");
            jdbcTemplate.update("DELETE FROM films");
            jdbcTemplate.update("DELETE FROM users");
            jdbcTemplate.update("DELETE FROM genres");
            jdbcTemplate.update("DELETE FROM mpa_ratings");

            // Сбрасываем последовательности для всех таблиц
            jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.execute("ALTER TABLE genres ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.execute("ALTER TABLE mpa_ratings ALTER COLUMN id RESTART WITH 1");

            // Включаем foreign key constraints обратно
            jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

            // Переинициализируем базовые данные
            initializeBaseData();

            log.info("=== DATABASE RESET COMPLETE ===");
            return "Database reset successfully";
        } catch (Exception e) {
            log.error("Error resetting database: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }

    private void initializeBaseData() {
        // Инициализируем MPA рейтинги
        String insertMpa = "MERGE INTO mpa_ratings (id, name) KEY (id) VALUES (?, ?)";
        jdbcTemplate.update(insertMpa, 1, "G");
        jdbcTemplate.update(insertMpa, 2, "PG");
        jdbcTemplate.update(insertMpa, 3, "PG-13");
        jdbcTemplate.update(insertMpa, 4, "R");
        jdbcTemplate.update(insertMpa, 5, "NC-17");

        // Инициализируем жанры
        String insertGenres = "MERGE INTO genres (id, name) KEY (id) VALUES (?, ?)";
        jdbcTemplate.update(insertGenres, 1, "Комедия");
        jdbcTemplate.update(insertGenres, 2, "Драма");
        jdbcTemplate.update(insertGenres, 3, "Мультфильм");
        jdbcTemplate.update(insertGenres, 4, "Триллер");
        jdbcTemplate.update(insertGenres, 5, "Документальный");
        jdbcTemplate.update(insertGenres, 6, "Боевик");
    }
}
