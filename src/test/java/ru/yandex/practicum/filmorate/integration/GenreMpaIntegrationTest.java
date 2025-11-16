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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDao;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDao;
import ru.yandex.practicum.filmorate.storage.user.UserDbStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.*;

@JdbcTest
@Import({GenreDao.class, MpaDao.class, FilmDbStorage.class, UserDbStorage.class})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
public class GenreMpaIntegrationTest {
    private final GenreDao genreDao;
    private final MpaDao mpaDao;
    private final FilmDbStorage filmDbStorage;
    private final UserDbStorage userDbStorage;
    private final JdbcTemplate jdbcTemplate;

    @BeforeEach
    public void setUp() {
        jdbcTemplate.update("DELETE FROM film_likes");
        jdbcTemplate.update("DELETE FROM film_genres");
        jdbcTemplate.update("DELETE FROM friendships");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");

        jdbcTemplate.update("ALTER TABLE films ALTER COLUMN id RESTART WITH 1");
        jdbcTemplate.update("ALTER TABLE users ALTER COLUMN id RESTART WITH 1");
    }

    @Test
    public void shouldFindAllGenres() {
        List<Genre> genres = genreDao.findAll();

        assertThat(genres).hasSize(6);
        assertThat(genres)
                .extracting(Genre::getName)
                .containsExactlyInAnyOrder(
                        "Комедия", "Драма", "Мультфильм",
                        "Триллер", "Документальный", "Боевик"
                );
    }

    @Test
    public void shouldFindGenreById() {
        Optional<Genre> genre = genreDao.findById(1);

        assertThat(genre)
                .isPresent()
                .hasValueSatisfying(g -> {
                    assertThat(g.getId()).isEqualTo(1);
                    assertThat(g.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    public void shouldReturnEmptyWhenGenreNotFound() {
        Optional<Genre> genre = genreDao.findById(999);

        assertThat(genre).isEmpty();
    }

    @Test
    public void shouldFindGenresByFilmId() {
        Film film = createTestFilmWithoutGenres();
        Film savedFilm = filmDbStorage.save(film);

        jdbcTemplate.update("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                savedFilm.getId(), 1);
        jdbcTemplate.update("INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)",
                savedFilm.getId(), 3);

        List<Genre> filmGenres = genreDao.findGenresByFilmId(savedFilm.getId());

        assertThat(filmGenres).hasSize(2);
        assertThat(filmGenres)
                .extracting(Genre::getId)
                .containsExactlyInAnyOrder(1, 3);
    }

    @Test
    public void shouldReturnEmptyListWhenFilmHasNoGenres() {
        Film film = createTestFilmWithoutGenres();
        Film savedFilm = filmDbStorage.save(film);

        List<Genre> filmGenres = genreDao.findGenresByFilmId(savedFilm.getId());

        assertThat(filmGenres).isEmpty();
    }

    @Test
    public void shouldFindAllMpaRatings() {
        List<Mpa> mpaRatings = mpaDao.findAll();

        assertThat(mpaRatings).hasSize(5);
        assertThat(mpaRatings)
                .extracting(Mpa::getName)
                .containsExactlyInAnyOrder("G", "PG", "PG-13", "R", "NC-17");
    }

    @Test
    public void shouldFindMpaById() {
        Optional<Mpa> mpa = mpaDao.findById(1);

        assertThat(mpa)
                .isPresent()
                .hasValueSatisfying(m -> {
                    assertThat(m.getId()).isEqualTo(1);
                    assertThat(m.getName()).isEqualTo("G");
                });
    }

    @Test
    public void shouldReturnEmptyWhenMpaNotFound() {

        Optional<Mpa> mpa = mpaDao.findById(999);

        assertThat(mpa).isEmpty();
    }

    @Test
    public void shouldSaveFilmWithMpaAndGenres() {
        Film film = createTestFilm();

        Film savedFilm = filmDbStorage.save(film);

        Optional<Film> foundFilm = filmDbStorage.findById(savedFilm.getId());
        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getMpa().getId()).isEqualTo(3);
                    assertThat(f.getMpa().getName()).isEqualTo("PG-13");
                    assertThat(f.getGenres()).hasSize(2);
                    assertThat(f.getGenres())
                            .extracting(Genre::getId)
                            .containsExactlyInAnyOrder(1, 2);
                });
    }

    @Test
    public void shouldUpdateFilmGenres() {
        Film film = createTestFilm();
        Film savedFilm = filmDbStorage.save(film);

        Set<Genre> newGenres = new HashSet<>();
        newGenres.add(new Genre(4, "Триллер"));
        newGenres.add(new Genre(6, "Боевик"));
        savedFilm.setGenres(newGenres);
        filmDbStorage.update(savedFilm);

        Optional<Film> updatedFilm = filmDbStorage.findById(savedFilm.getId());
        assertThat(updatedFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f.getGenres()).hasSize(2);
                    assertThat(f.getGenres())
                            .extracting(Genre::getId)
                            .containsExactlyInAnyOrder(4, 6);
                });
    }

    @Test
    public void shouldHandleFilmWithNoGenres() {
        Film film = createTestFilmWithoutGenres();

        Film savedFilm = filmDbStorage.save(film);

        Optional<Film> foundFilm = filmDbStorage.findById(savedFilm.getId());
        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f ->
                        assertThat(f.getGenres()).isEmpty()
                );
    }

    @Test
    public void shouldFindPopularFilmsWithLikesAndGenres() {
        Film film1 = createTestFilm();
        film1.setName("Film One");
        Film savedFilm1 = filmDbStorage.save(film1);

        Film film2 = createTestFilm();
        film2.setName("Film Two");
        Film savedFilm2 = filmDbStorage.save(film2);

        User user = createTestUser();
        User savedUser = userDbStorage.save(user);

        filmDbStorage.addLike(savedFilm1.getId(), savedUser.getId());

        List<Film> popularFilms = filmDbStorage.findPopularFilms(2);

        assertThat(popularFilms).hasSize(2);

        Film popularFilm1 = popularFilms.get(0);
        Film popularFilm2 = popularFilms.get(1);

        boolean film1HasLike = popularFilm1.getLikes().contains(savedUser.getId()) ||
                popularFilm2.getLikes().contains(savedUser.getId());
        assertThat(film1HasLike).isTrue();
    }

    @Test
    public void shouldThrowExceptionWhenFilmMpaNotFound() {
        Film film = createTestFilmWithoutGenres();
        film.setMpa(new Mpa(999, "Non-existent"));

        assertThatThrownBy(() -> filmDbStorage.save(film))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    public void shouldThrowExceptionWhenFilmGenreNotFound() {
        Film film = createTestFilmWithoutGenres();
        Set<Genre> genres = new HashSet<>();
        genres.add(new Genre(999, "Non-existent"));
        film.setGenres(genres);

        assertThatThrownBy(() -> filmDbStorage.save(film))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Film createTestFilm() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(3, "PG-13"));

        Set<Genre> genres = new HashSet<>();
        genres.add(new Genre(1, "Комедия"));
        genres.add(new Genre(2, "Драма"));
        film.setGenres(genres);

        return film;
    }

    private Film createTestFilmWithoutGenres() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("Test Description");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(120);
        film.setMpa(new Mpa(3, "PG-13"));
        film.setGenres(new HashSet<>()); // Пустой набор жанров

        return film;
    }

    private User createTestUser() {
        User user = new User();
        user.setEmail("test@mail.ru");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(LocalDate.of(2000, 1, 1));
        return user;
    }
}
