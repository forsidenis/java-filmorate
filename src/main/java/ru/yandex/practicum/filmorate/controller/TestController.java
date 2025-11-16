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
            jdbcTemplate.update("DELETE FROM film_likes");
            jdbcTemplate.update("DELETE FROM film_genres");
            jdbcTemplate.update("DELETE FROM friendships");
            jdbcTemplate.update("DELETE FROM films");
            jdbcTemplate.update("DELETE FROM users");

            jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
            jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

            log.info("=== DATABASE RESET COMPLETE ===");
            return "Database reset successfully";
        } catch (Exception e) {
            log.error("Error resetting database: {}", e.getMessage());
            return "Error: " + e.getMessage();
        }
    }
}
