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
public class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final AtomicInteger counter = new AtomicInteger(0);

    @BeforeEach
    public void setUp() {
        try {
            mockMvc.perform(post("/test/reset"));
        } catch (Exception e) {
            manualReset();
        }
    }

    private void manualReset() {
        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY FALSE");

        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

        jdbcTemplate.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }

    private String getUniqueEmail() {
        return "test" + counter.incrementAndGet() + "@mail.ru";
    }

    private String getUniqueLogin() {
        return "testlogin" + counter.incrementAndGet();
    }

    @Test
    public void shouldCreateValidUser() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin(getUniqueLogin());
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.login").value(user.getLogin()))
                .andExpect(jsonPath("$.name").value(user.getLogin())); // Должен использовать логин как имя
    }

    @Test
    public void shouldCreateUserWithName() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin(getUniqueLogin());
        user.setName("Тестовое Имя");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Тестовое Имя"));
    }

    @Test
    public void shouldReturnBadRequestForInvalidUser() throws Exception {
        User user = new User();
        user.setEmail("invalid-email"); // Невалидный email
        user.setLogin(""); // Пустой логин
        user.setBirthday(LocalDate.now().plusDays(1)); // Дата рождения в будущем

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturnBadRequestForLoginWithSpaces() throws Exception {
        User user = new User();
        user.setEmail(getUniqueEmail());
        user.setLogin("login with spaces"); // Логин с пробелами
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest());
    }
}
