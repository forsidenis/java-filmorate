package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.main.banner-mode=off",
        "logging.level.org.springframework=ERROR",
        "logging.level.com.zaxxer.hikari=ERROR"
})
public class FilmorateApplicationTests {

    @Autowired
    private Validator validator;

    private Film validFilm;
    private User validUser;

    @BeforeEach
    public void setUp() {
        validFilm = new Film();
        validFilm.setName("Valid Film");
        validFilm.setDescription("Valid Description");
        validFilm.setReleaseDate(LocalDate.of(2000, 1, 1));
        validFilm.setDuration(120);

        validUser = new User();
        validUser.setEmail("valid@email.com");
        validUser.setLogin("validlogin");
        validUser.setBirthday(LocalDate.of(2000, 1, 1));
    }

    @Test
    public void contextLoads() {
        assertNotNull(validator);
    }

    @Test
    public void shouldCreateValidFilm() {
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateFilmWithEmptyName() {
        validFilm.setName("");
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateFilmWithTooLongDescription() {
        validFilm.setDescription("A".repeat(201));
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateFilmWithNegativeDuration() {
        validFilm.setDuration(-1);
        Set<ConstraintViolation<Film>> violations = validator.validate(validFilm);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldCreateValidUser() {
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateUserWithInvalidEmail() {
        validUser.setEmail("invalid-email");
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateUserWithEmptyEmail() {
        validUser.setEmail("");
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateUserWithEmptyLogin() {
        validUser.setLogin("");
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateUserWithLoginContainingSpaces() {
        validUser.setLogin("login with spaces");
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertFalse(violations.isEmpty());
    }

    @Test
    public void shouldNotCreateUserWithFutureBirthday() {
        validUser.setBirthday(LocalDate.now().plusDays(1));
        Set<ConstraintViolation<User>> violations = validator.validate(validUser);
        assertFalse(violations.isEmpty());
    }
}
