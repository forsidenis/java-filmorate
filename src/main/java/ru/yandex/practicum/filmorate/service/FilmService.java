package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreStorage genreStorage;
    private final JdbcTemplate jdbcTemplate;

    public List<Film> findAll() {
        List<Film> films = filmStorage.findAll();
        loadGenresForFilms(films);
        return films;
    }

    public Film findById(Integer id) {
        Film film = filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
        // Загружаем жанры для конкретного фильма
        film.setGenres(genreStorage.getGenresByFilmId(film.getId()));
        return film;
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());

        try {
            Film createdFilm = filmStorage.save(film);
            // Загружаем жанры для созданного фильма
            createdFilm.setGenres(genreStorage.getGenresByFilmId(createdFilm.getId()));
            return createdFilm;
        } catch (DataIntegrityViolationException e) {
            // Обрабатываем случай, когда жанр не существует в базе
            if (e.getMessage().contains("GENRE") || e.getMessage().contains("genre") ||
                    e.getMessage().contains("foreign key")) {
                throw new NotFoundException("Один из указанных жанров не найден");
            }
            throw e;
        }
    }

    public Film update(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("ID фильма не может быть пустым");
        }

        // Проверяем, что фильм существует
        findById(film.getId());

        validateFilm(film);
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());

        try {
            Film updatedFilm = filmStorage.update(film);
            // Загружаем жанры для обновленного фильма
            updatedFilm.setGenres(genreStorage.getGenresByFilmId(updatedFilm.getId()));
            return updatedFilm;
        } catch (DataIntegrityViolationException e) {
            // Обрабатываем случай, когда жанр не существует в базе
            if (e.getMessage().contains("GENRE") || e.getMessage().contains("genre") ||
                    e.getMessage().contains("foreign key")) {
                throw new NotFoundException("Один из указанных жанров не найден");
            }
            throw e;
        }
    }

    public void addLike(Integer filmId, Integer userId) {
        // Проверяем существование пользователя
        userService.findById(userId);
        // Проверяем существование фильма
        findById(filmId);

        filmStorage.addLike(filmId, userId);
        log.info("Пользователь {} поставил лайк фильму {}", userId, filmId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        // Проверяем существование пользователя
        userService.findById(userId);
        // Проверяем существование фильма
        findById(filmId);

        filmStorage.removeLike(filmId, userId);
        log.info("Пользователь {} удалил лайк с фильма {}", userId, filmId);
    }

    public List<Film> findPopularFilms(Integer count) {
        int filmsCount = count != null ? count : 10;
        if (filmsCount <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        List<Film> films = filmStorage.findPopularFilms(filmsCount);
        loadGenresForFilms(films);
        return films;
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(FilmController.CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateMpa(Mpa mpa) {
        if (mpa == null) {
            throw new ValidationException("Рейтинг MPA не может быть пустым");
        }
        if (mpa.getId() == null) {
            throw new ValidationException("ID рейтинга MPA не может быть пустым");
        }

        mpaService.getMpaById(mpa.getId());
    }

    private void validateGenres(List<Genre> genres) {
        if (genres != null) {
            for (Genre genre : genres) {
                if (genre.getId() == null) {
                    throw new ValidationException("ID жанра не может быть пустым");
                }

                genreStorage.getGenreById(genre.getId());
            }
        }
    }

    private void loadGenresForFilms(List<Film> films) {
        List<Integer> filmIds = films.stream()
                .map(Film::getId)
                .collect(Collectors.toList());

        if (filmIds.isEmpty()) {
            return;
        }

        // Создаем IN clause с нужным количеством параметров
        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = String.format(
                "SELECT fg.film_id, g.id, g.name " +
                        "FROM film_genres fg " +
                        "JOIN genres g ON fg.genre_id = g.id " +
                        "WHERE fg.film_id IN (%s) " +
                        "ORDER BY fg.film_id, g.id", inClause
        );

        // Выполняем запрос и маппим результаты
        Map<Integer, List<Genre>> genresByFilmId = new HashMap<>();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, filmIds.toArray());
        for (Map<String, Object> row : rows) {
            Integer filmId = (Integer) row.get("film_id");
            Genre genre = new Genre((Integer) row.get("id"), (String) row.get("name"));
            genresByFilmId.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
        }

        // Устанавливаем жанры для каждого фильма
        films.forEach(film -> {
            List<Genre> genres = genresByFilmId.getOrDefault(film.getId(), new ArrayList<>());
            film.setGenres(genres);
        });
    }
}
