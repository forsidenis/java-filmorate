package ru.yandex.practicum.filmorate.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserDbStorageTest {
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    private User testUser1;
    private User testUser2;
    private User testUser3;

    @BeforeEach
    void setUp() {
        // Очищаем базу данных перед каждым тестом
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM users");

        // Сбрасываем автоинкремент
        jdbcTemplate.execute("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");

        // Инициализируем тестовых пользователей
        testUser1 = new User();
        testUser1.setEmail("user1@mail.ru");
        testUser1.setLogin("user1");
        testUser1.setName("User One");
        testUser1.setBirthday(LocalDate.of(1990, 1, 1));

        testUser2 = new User();
        testUser2.setEmail("user2@mail.ru");
        testUser2.setLogin("user2");
        testUser2.setName("User Two");
        testUser2.setBirthday(LocalDate.of(1995, 5, 5));

        testUser3 = new User();
        testUser3.setEmail("user3@mail.ru");
        testUser3.setLogin("user3");
        testUser3.setName("User Three");
        testUser3.setBirthday(LocalDate.of(2000, 10, 10));
    }

    @Test
    public void shouldSaveAndFindUserById() {
        User savedUser = userDbStorage.save(testUser1);

        Optional<User> foundUser = userDbStorage.findById(savedUser.getId());

        assertThat(foundUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getId()).isEqualTo(savedUser.getId());
                    assertThat(user.getEmail()).isEqualTo("user1@mail.ru");
                    assertThat(user.getLogin()).isEqualTo("user1");
                    assertThat(user.getName()).isEqualTo("User One");
                    assertThat(user.getBirthday()).isEqualTo(LocalDate.of(1990, 1, 1));
                });
    }

    @Test
    public void shouldReturnEmptyWhenUserNotFound() {
        Optional<User> foundUser = userDbStorage.findById(9999);

        assertThat(foundUser).isEmpty();
    }

    @Test
    public void shouldFindAllUsers() {
        userDbStorage.save(testUser1);
        userDbStorage.save(testUser2);

        List<User> users = userDbStorage.findAll();

        assertThat(users).hasSize(2);
        assertThat(users)
                .extracting(User::getLogin)
                .containsExactlyInAnyOrder("user1", "user2");
    }

    @Test
    public void shouldUpdateUser() {
        User savedUser = userDbStorage.save(testUser1);
        savedUser.setEmail("updated@mail.ru");
        savedUser.setLogin("updatedlogin");
        savedUser.setName("Updated Name");

        User updatedUser = userDbStorage.update(savedUser);

        assertThat(updatedUser.getEmail()).isEqualTo("updated@mail.ru");
        assertThat(updatedUser.getLogin()).isEqualTo("updatedlogin");
        assertThat(updatedUser.getName()).isEqualTo("Updated Name");

        Optional<User> dbUser = userDbStorage.findById(savedUser.getId());
        assertThat(dbUser)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user.getEmail()).isEqualTo("updated@mail.ru");
                    assertThat(user.getLogin()).isEqualTo("updatedlogin");
                });
    }

    @Test
    public void shouldThrowExceptionWhenUpdatingNonExistentUser() {
        testUser1.setId(9999);

        assertThatThrownBy(() -> userDbStorage.update(testUser1))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с ID 9999 не найден");
    }

    @Test
    public void shouldDeleteUser() {
        User savedUser = userDbStorage.save(testUser1);

        userDbStorage.delete(savedUser.getId());

        Optional<User> foundUser = userDbStorage.findById(savedUser.getId());
        assertThat(foundUser).isEmpty();
    }

    @Test
    public void shouldAddFriend() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);

        userDbStorage.addFriend(user1.getId(), user2.getId());

        List<User> friends = userDbStorage.findFriends(user1.getId());
        assertThat(friends).hasSize(1);
        assertThat(friends.get(0).getId()).isEqualTo(user2.getId());

        List<User> user2Friends = userDbStorage.findFriends(user2.getId());
        assertThat(user2Friends).isEmpty();
    }

    @Test
    public void shouldThrowExceptionWhenAddingFriendToNonExistentUser() {
        User savedUser = userDbStorage.save(testUser1);

        assertThatThrownBy(() -> userDbStorage.addFriend(savedUser.getId(), 9999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с ID 9999 не найден");
    }

    @Test
    public void shouldRemoveFriend() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);
        userDbStorage.addFriend(user1.getId(), user2.getId());

        userDbStorage.removeFriend(user1.getId(), user2.getId());

        List<User> friends = userDbStorage.findFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    public void shouldRemoveFriendWithoutExceptionWhenFriendshipDoesNotExist() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);

        assertThatNoException().isThrownBy(() -> userDbStorage.removeFriend(user1.getId(), user2.getId()));

        List<User> friends = userDbStorage.findFriends(user1.getId());
        assertThat(friends).isEmpty();
    }

    @Test
    public void shouldFindFriends() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);
        User user3 = userDbStorage.save(testUser3);

        userDbStorage.addFriend(user1.getId(), user2.getId());
        userDbStorage.addFriend(user1.getId(), user3.getId());

        List<User> friends = userDbStorage.findFriends(user1.getId());

        assertThat(friends).hasSize(2);
        assertThat(friends)
                .extracting(User::getId)
                .containsExactlyInAnyOrder(user2.getId(), user3.getId());
    }

    @Test
    public void shouldThrowExceptionWhenFindingFriendsOfNonExistentUser() {
        assertThatThrownBy(() -> userDbStorage.findFriends(9999))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь с ID 9999 не найден");
    }

    @Test
    public void shouldFindCommonFriends() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);
        User user3 = userDbStorage.save(testUser3);

        userDbStorage.addFriend(user1.getId(), user3.getId());
        userDbStorage.addFriend(user2.getId(), user3.getId());

        List<User> commonFriends = userDbStorage.findCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).hasSize(1);
        assertThat(commonFriends.get(0).getId()).isEqualTo(user3.getId());
    }

    @Test
    public void shouldReturnEmptyListWhenNoCommonFriends() {
        User user1 = userDbStorage.save(testUser1);
        User user2 = userDbStorage.save(testUser2);
        User user3 = userDbStorage.save(testUser3);

        userDbStorage.addFriend(user1.getId(), user3.getId());

        List<User> commonFriends = userDbStorage.findCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends).isEmpty();
    }

    @Test
    public void shouldPreventDuplicateEmails() {
        userDbStorage.save(testUser1);

        User duplicateEmailUser = new User();
        duplicateEmailUser.setEmail("user1@mail.ru"); // Same email
        duplicateEmailUser.setLogin("differentlogin");
        duplicateEmailUser.setName("Different Name");
        duplicateEmailUser.setBirthday(LocalDate.of(2000, 1, 1));

        assertThatThrownBy(() -> userDbStorage.save(duplicateEmailUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void shouldPreventDuplicateLogins() {
        userDbStorage.save(testUser1);

        User duplicateLoginUser = new User();
        duplicateLoginUser.setEmail("different@mail.ru");
        duplicateLoginUser.setLogin("user1"); // Same login
        duplicateLoginUser.setName("Different Name");
        duplicateLoginUser.setBirthday(LocalDate.of(2000, 1, 1));

        assertThatThrownBy(() -> userDbStorage.save(duplicateLoginUser))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
