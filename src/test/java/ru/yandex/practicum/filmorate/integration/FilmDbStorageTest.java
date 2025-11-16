package ru.yandex.practicum.filmorate.integration;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDao;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDao;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({FilmDbStorage.class, GenreDao.class, MpaDao.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class FilmDbStorageTest {
    private final FilmDbStorage filmDbStorage;
    private final JdbcTemplate jdbcTemplate;
    private final MpaDao mpaDao;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM mpa_ratings");
        jdbcTemplate.update("DELETE FROM genres");

        // Инициализируем данные
        jdbcTemplate.update("INSERT INTO mpa_ratings (id, name) VALUES (1, 'G')");
        jdbcTemplate.update("INSERT INTO genres (id, name) VALUES (1, 'Комедия')");
    }

    @Test
    public void shouldFindFilmById() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));

        Film savedFilm = filmDbStorage.save(film);
        Optional<Film> foundFilm = filmDbStorage.findById(savedFilm.getId());

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f).hasFieldOrPropertyWithValue("name", "Test Film")
                );
    }

    @Test
    public void shouldSaveAndUpdateFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(1, "G"));

        Film savedFilm = filmDbStorage.save(film);
        savedFilm.setName("Updated Film");

        Film updatedFilm = filmDbStorage.update(savedFilm);

        assertThat(updatedFilm.getName()).isEqualTo("Updated Film");
    }
}
