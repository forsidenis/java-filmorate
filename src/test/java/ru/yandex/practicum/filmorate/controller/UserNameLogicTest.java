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
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class UserNameLogicTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger counter = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        // Очищаем базу перед каждым тестом
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    private String getUniqueEmail() {
        return "test" + counter.incrementAndGet() + "@mail.ru";
    }

    private String getUniqueLogin() {
        return "testlogin" + counter.incrementAndGet();
    }

    @Test
    public void shouldUseLoginAsNameWhenNameIsNull() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin(getUniqueLogin());
        user.setName(null); // Явно null
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(user.getLogin()));
    }

    @Test
    public void shouldUseLoginAsNameWhenNameIsBlank() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin(getUniqueLogin());
        user.setName("   "); // Пробелы
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(user.getLogin()));
    }

    @Test
    public void shouldKeepNameWhenNameIsProvided() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin(getUniqueLogin());
        user.setName("Реальное Имя");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Реальное Имя"));
    }
}
