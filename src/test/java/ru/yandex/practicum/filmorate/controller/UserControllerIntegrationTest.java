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
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.util.TestDatabaseCleaner;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
@ActiveProfiles("test") // Активируем тестовый профиль
public class UserControllerIntegrationTest {

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
    public void shouldCreateValidUser() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setEmail("test" + uniqueId + "@mail.ru");
        user.setLogin("testlogin" + uniqueId);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.login").value("testlogin" + uniqueId))
                .andExpect(jsonPath("$.name").value("testlogin" + uniqueId));
    }
}
