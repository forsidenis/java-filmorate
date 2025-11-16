package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserService userService;
    private final MpaStorage mpaStorage;
    private final GenreStorage genreStorage;

    public List<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Integer id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + id + " не найден"));
    }

    public Film create(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa().getId());
        validateGenres(film);
        return filmStorage.save(film);
    }

    public Film update(Film film) {
        validateFilm(film);
        validateMpa(film.getMpa().getId());
        validateGenres(film);
        filmStorage.findById(film.getId())
                .orElseThrow(() -> new NotFoundException("Фильм с ID " + film.getId() + " не найден"));
        return filmStorage.update(film);
    }

    public void addLike(Integer filmId, Integer userId) {
        userService.findById(userId); // Проверяем существование пользователя
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Integer filmId, Integer userId) {
        userService.findById(userId); // Проверяем существование пользователя
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> findPopularFilms(Integer count) {
        int filmsCount = (count == null || count <= 0) ? 10 : count;
        return filmStorage.findPopularFilms(filmsCount);
    }

    private void validateFilm(Film film) {
        if (film.getReleaseDate().isBefore(FilmController.CINEMA_BIRTHDAY)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
    }

    private void validateMpa(Integer mpaId) {
        if (mpaId != null) {
            mpaStorage.findById(mpaId)
                    .orElseThrow(() -> new NotFoundException("MPA с ID " + mpaId + " не найден"));
        }
    }

    private void validateGenres(Film film) {
        if (film.getGenres() != null) {
            for (var genre : film.getGenres()) {
                genreStorage.findById(genre.getId())
                        .orElseThrow(() -> new NotFoundException("Жанр с ID " + genre.getId() + " не найден"));
            }
        }
    }
}
