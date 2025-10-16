package ru.yandex.practicum.filmorate.model;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class FilmBoundaryTests {
    private Validator validator;

    @BeforeEach
    void setUp() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
    }

    @Test
    void shouldValidateFilmWithExact200CharDescription() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("A".repeat(200)); // Ровно 200 символов
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Описание длиной 200 символов должно быть валидным");
    }

    @Test
    void shouldNotValidateFilmWith201CharDescription() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("A".repeat(201)); // 201 символ - слишком много
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Описание длиной 201 символ должно быть невалидным");
    }

    @Test
    void shouldValidateFilmWithCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1895, 12, 28)); // Минимальная допустимая дата
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Дата релиза 28.12.1895 должна быть валидной");
    }

    @Test
    void shouldNotValidateFilmBeforeCinemaBirthday() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(1895, 12, 27)); // На день раньше
        film.setDuration(120);

        Set<ConstraintViolation<Film>> violations = validator.validate(film);

        assertFalse(violations.isEmpty(), "Дата релиза до 28.12.1895 должна быть невалидной");

        Optional<String> errorMessage = violations.stream()
                .map(ConstraintViolation::getMessage)
                .findFirst();
        assertTrue(errorMessage.isPresent());
        assertTrue(errorMessage.get().contains("28 декабря 1895"));
    }

    @Test
    void shouldValidateFilmWithDuration1() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(1); // Минимальная положительная продолжительность

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertTrue(violations.isEmpty(), "Продолжительность 1 должна быть валидной");
    }

    @Test
    void shouldNotValidateFilmWithZeroDuration() {
        Film film = new Film();
        film.setName("Фильм");
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(0); // Нулевая продолжительность

        Set<ConstraintViolation<Film>> violations = validator.validate(film);
        assertFalse(violations.isEmpty(), "Продолжительность 0 должна быть невалидной");
    }
}