package ru.yandex.practicum.filmorate.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserDbStorageTest {
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    public void shouldSaveAndFindUserById() {
        User user = new User();
        user.setEmail("user1@mail.ru");
        user.setLogin("user1");
        user.setName("User One");
        user.setBirthday(LocalDate.of(1990, 1, 1));

        User savedUser = userDbStorage.save(user);
        Optional<User> foundUser = userDbStorage.findById(savedUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u.getId()).isEqualTo(savedUser.getId());
                    assertThat(u.getEmail()).isEqualTo("user1@mail.ru");
                    assertThat(u.getLogin()).isEqualTo("user1");
                    assertThat(u.getName()).isEqualTo("User One");
                });
    }

    @Test
    public void shouldFindAllUsers() {
        User user1 = createTestUser("user1@mail.ru", "user1");
        User user2 = createTestUser("user2@mail.ru", "user2");

        userDbStorage.save(user1);
        userDbStorage.save(user2);

        List<User> users = userDbStorage.findAll();
        assertThat(users).hasSize(2);
    }

    @Test
    public void shouldAddFriend() {
        User user1 = createTestUser("user1@mail.ru", "user1");
        User user2 = createTestUser("user2@mail.ru", "user2");

        User savedUser1 = userDbStorage.save(user1);
        User savedUser2 = userDbStorage.save(user2);

        userDbStorage.addFriend(savedUser1.getId(), savedUser2.getId());

        List<User> friends = userDbStorage.findFriends(savedUser1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(savedUser2.getId());
    }

    private User createTestUser(String email, String login) {
        User user = new User();
        user.setEmail(email);
        user.setLogin(login);
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
