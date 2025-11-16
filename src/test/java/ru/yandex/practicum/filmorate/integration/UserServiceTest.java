package ru.yandex.practicum.filmorate.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.UserService;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({UserService.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class UserServiceTest {
    private final UserService userService;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        // Создаем только таблицу users для этого теста
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTO_INCREMENT, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "login VARCHAR(255) NOT NULL UNIQUE, " +
                "name VARCHAR(255), " +
                "birthday DATE NOT NULL)");

        // Очищаем данные
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    public void shouldSetLoginAsNameWhenNameIsNull() {
        User user = new User();
        user.setEmail("noname@mail.ru");
        user.setLogin("nonameuser");
        user.setName(null); // Явно null
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userService.create(user);

        assertThat(savedUser.getName()).isEqualTo("nonameuser");
    }

    @Test
    public void shouldSetLoginAsNameWhenNameIsBlank() {
        User user = new User();
        user.setEmail("blankname@mail.ru");
        user.setLogin("blankuser");
        user.setName("   "); // Пробелы
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userService.create(user);

        assertThat(savedUser.getName()).isEqualTo("blankuser");
    }

    @Test
    public void shouldKeepNameWhenNameIsProvided() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Реальное Имя");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        User savedUser = userService.create(user);

        assertThat(savedUser.getName()).isEqualTo("Реальное Имя");
    }

    @Test
    public void shouldUpdateUserNameLogic() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setName("Original Name");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        User savedUser = userService.create(user);

        savedUser.setName("   ");
        User updatedUser = userService.update(savedUser);

        assertThat(updatedUser.getName()).isEqualTo("testlogin");
    }
}
