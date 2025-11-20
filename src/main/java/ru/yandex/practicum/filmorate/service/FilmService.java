package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaService mpaService;
    private final GenreService genreService;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa());
        validateGenres(film.getGenres());

        return filmStorage.save(film);
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

        return filmStorage.update(film);
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
        return filmStorage.findPopularFilms(filmsCount);
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

        // Проверяем существование MPA
        try {
            mpaService.getMpaById(mpa.getId());
        } catch (NotFoundException e) {
            throw new ValidationException("Рейтинг MPA с ID " + mpa.getId() + " не найден");
        }
    }

    private void validateGenres(List<Genre> genres) {
        if (genres != null) {
            for (Genre genre : genres) {
                if (genre.getId() == null) {
                    throw new ValidationException("ID жанра не может быть пустым");
                }
                // Проверяем существование жанра
                genreService.getGenreById(genre.getId());
            }
        }
    }
}
