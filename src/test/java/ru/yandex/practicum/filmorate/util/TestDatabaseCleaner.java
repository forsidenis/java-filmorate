package ru.yandex.practicum.filmorate.util;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@Component
public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    private final List<String> tables = List.of("film_likes", "film_genres", "friends", "films", "users");

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void cleanDatabase() {
        // Отключаем внешние ключи для H2
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        for (String table : tables) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE " + table);
                // Сбрасываем автоинкремент для таблиц, у которых есть ID
                if (table.equals("films") || table.equals("users")) {
                    jdbcTemplate.execute("ALTER TABLE " + table + " ALTER COLUMN ID RESTART WITH 1");
                }
            } catch (Exception e) {
                System.out.println("Warning: Could not clean table " + table + ": " + e.getMessage());
            }
        }

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
}
