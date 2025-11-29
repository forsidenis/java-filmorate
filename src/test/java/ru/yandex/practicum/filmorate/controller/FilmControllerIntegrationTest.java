package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.util.TestDatabaseCleaner;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class FilmControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    public void setUp() {
        testDatabaseCleaner.cleanDatabase();
    }

    @Test
    public void shouldCreateValidFilm() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        Film film = new Film();
        film.setName("Тестовый фильм " + uniqueId);
        film.setDescription("Тестовое описание " + uniqueId);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Тестовый фильм " + uniqueId));
    }

    @Test
    public void shouldReturnBadRequestForInvalidFilm() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        Film film = new Film();
        film.setName(""); // Пустое название
        film.setDescription("A".repeat(201)); // Слишком длинное описание
        film.setReleaseDate(LocalDate.of(1890, 1, 1)); // Дата до 1895-12-28
        film.setDuration(-10); // Отрицательная продолжительность

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldUpdateFilm() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Сначала создаем фильм
        Film film = new Film();
        film.setName("Старое название " + uniqueId);
        film.setDescription("Описание " + uniqueId);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andReturn().getResponse().getContentAsString();

        Film createdFilm = objectMapper.readValue(response, Film.class);

        // Обновляем фильм
        createdFilm.setName("Новое название " + uniqueId);

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createdFilm)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Новое название " + uniqueId));
    }

    @Test
    public void shouldReturnNotFoundWhenUpdatingNonExistentFilm() throws Exception {
        Film film = new Film();
        film.setId(9999);
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Mpa mpa = new Mpa();
        mpa.setId(1);
        film.setMpa(mpa);

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ресурс не найден"));
    }

    @Test
    public void shouldGetAllFilms() throws Exception {
        mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
