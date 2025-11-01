package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class FilmFriendsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldAddAndRemoveFriends() throws Exception {
        // Создаем двух пользователей
        User user1 = createUser("user1@mail.ru", "user1");
        User user2 = createUser("user2@mail.ru", "user2");

        Integer user1Id = user1.getId();
        Integer user2Id = user2.getId();

        // Добавляем в друзья
        mockMvc.perform(put("/users/{id}/friends/{friendId}", user1Id, user2Id))
                .andExpect(status().isOk());

        // Проверяем список друзей
        mockMvc.perform(get("/users/{id}/friends", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(user2Id));

        // Удаляем из друзей
        mockMvc.perform(delete("/users/{id}/friends/{friendId}", user1Id, user2Id))
                .andExpect(status().isOk());

        // Проверяем, что друзей нет
        mockMvc.perform(get("/users/{id}/friends", user1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    public void shouldHandleLikes() throws Exception {
        // Создаем фильм и пользователя
        Film film = createFilm("Test Film", "Description");
        User user = createUser("test@mail.ru", "testuser");

        Integer filmId = film.getId();
        Integer userId = user.getId();

        // Добавляем лайк
        mockMvc.perform(put("/films/{id}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());

        // Проверяем популярные фильмы
        mockMvc.perform(get("/films/popular?count=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(filmId));

        // Удаляем лайк
        mockMvc.perform(delete("/films/{id}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());
    }

    private User createUser(String email, String login) throws Exception {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setBirthday(LocalDate.of(2000, 1, 1));

        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, User.class);
    }

    private Film createFilm(String name, String description) throws Exception {
        Film film = new Film();
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(film)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readValue(response, Film.class);
    }
}
