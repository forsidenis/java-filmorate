package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class UserBoundaryTests {
    private Validator validator;

    @BeforeEach
    public void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    public void shouldValidateUserWithTodayBirthday() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.now().minusDays(1)); // Используем вчерашнюю дату

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Дата рождения в прошлом должна быть валидной");
    }

    @Test
    public void shouldNotValidateUserWithTomorrowBirthday() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.now().plusDays(1)); // Завтрашняя дата

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Дата рождения в будущем должна быть невалидной");
    }

    @Test
    public void shouldValidateUserWithValidEmailFormat() {
        User user = new User();
        user.setEmail("test.user+tag@example.co.uk"); // Сложный валидный email
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Сложный валидный email должен проходить проверку");
    }

    @Test
    public void shouldNotValidateUserWithEmailWithoutAt() {
        User user = new User();
        user.setEmail("invalid-email"); // Email без @
        user.setLogin("testlogin");
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Email без @ должен быть невалидным");
    }

    @Test
    public void shouldValidateUserWithComplexLogin() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("user_123-login"); // Логин с подчеркиваниями и дефисами
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertTrue(violations.isEmpty(), "Логин с подчеркиваниями и дефисами должен быть валидным");
    }

    @Test
    public void shouldNotValidateUserWithLoginContainingSpaces() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("user login"); // Логин с пробелом
        user.setBirthday(LocalDate.of(2000, 1, 1));

        Set<ConstraintViolation<User>> violations = validator.validate(user);
        assertFalse(violations.isEmpty(), "Логин с пробелами должен быть невалидным");
    }
}
