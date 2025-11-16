package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FilmControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        // Сначала вызываем reset endpoint для полного сброса
        try {
            mockMvc.perform(post("/test/reset"));
        } catch (Exception e) {
            // Если endpoint недоступен, делаем сброс вручную
            manualReset();
        }
    }

    private void manualReset() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.execute("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");

        // Инициализируем MPA если нужно
        jdbcTemplate.update("MERGE INTO mpa_ratings (id, name) VALUES (1, 'G')");
        jdbcTemplate.update("MERGE INTO genres (id, name) VALUES (1, 'Комедия')");
    }

    @Test
    public void shouldCreateValidFilm() throws Exception {
        Film film = new Film();
        film.setName("Тестовый фильм");
        film.setDescription("Тестовое описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Тестовый фильм"));
    }

    @Test
    public void shouldReturnBadRequestForInvalidFilm() throws Exception {
        Film film = new Film();
        film.setName(""); // Пустое название
        film.setDescription("A".repeat(201)); // Слишком длинное описание
        film.setReleaseDate(LocalDate.of(1890, 1, 1)); // Дата до 1895-12-28
        film.setDuration(-10); // Отрицательная продолжительность
        film.setMpa(new Mpa(1, "G"));

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateFilm() throws Exception {
        // Сначала создаем фильм
        Film film = new Film();
        film.setName("Старое название");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andReturn().getResponse().getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);

        // Обновляем фильм
        createdFilm.setName("Новое название");

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое название"));
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingNonExistentFilm() throws Exception {
        Film film = new Film();
        film.setId(9999);
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isNotFound()) // Ожидаем 404
                .andExpect(jsonPath("$.error").value("Ресурс не найден"));
    }

    @Test
    public void shouldGetAllFilms() throws Exception {
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
