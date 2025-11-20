package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yandex.practicum.filmorate.util.TestDatabaseCleaner;

import java.time.LocalDate;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.main.banner-mode=off"})
@AutoConfigureMockMvc
public class UserNameLogicTest {

    @Autowired
    private TestDatabaseCleaner testDatabaseCleaner;

    @BeforeEach
    public void setUp() {
        testDatabaseCleaner.cleanDatabase();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldUseLoginAsNameWhenNameIsNull() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setEmail("test" + uniqueId + "@mail.ru");
        user.setLogin("testlogin" + uniqueId);
        user.setName(null); // Явно null
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testlogin" + uniqueId));
    }

    @Test
    public void shouldUseLoginAsNameWhenNameIsBlank() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setEmail("test" + uniqueId + "@mail.ru");
        user.setLogin("testlogin" + uniqueId);
        user.setName("   "); // Пробелы
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("testlogin" + uniqueId));
    }

    @Test
    public void shouldKeepNameWhenNameIsProvided() throws Exception {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User();
        user.setEmail("test" + uniqueId + "@mail.ru");
        user.setLogin("testlogin" + uniqueId);
        user.setName("Реальное Имя");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Реальное Имя"));
    }
}
