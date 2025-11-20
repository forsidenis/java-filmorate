package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;


@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({UserDbStorage.class})
public class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Test
    public void testFindUserById() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User createdUser = userStorage.save(user);
        Optional<User> userOptional = userStorage.findById(createdUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(u ->
                        assertThat(u).hasFieldOrPropertyWithValue("id", createdUser.getId())
                );
    }

    @Test
    public void testSaveAndFindUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userStorage.save(user);

        assertThat(savedUser.getId()).isNotNull();
        assertThat(savedUser.getEmail()).isEqualTo("test@mail.ru");
        assertThat(savedUser.getLogin()).isEqualTo("testlogin");
        assertThat(savedUser.getName()).isEqualTo("Test User");
    }

    @Test
    public void testUpdateUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userStorage.save(user);

        savedUser.setName("Updated User");
        savedUser.setEmail("updated@mail.ru");

        User updatedUser = userStorage.update(savedUser);

        assertThat(updatedUser.getName()).isEqualTo("Updated User");
        assertThat(updatedUser.getEmail()).isEqualTo("updated@mail.ru");
    }
}
