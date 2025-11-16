package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.genre.GenreStorage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Repository
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final GenreStorage genreStorage;

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f JOIN mpa_ratings m ON f.mpa_id = m.id";
        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper());
        setGenresForFilms(films);
        setLikesForFilms(films);
        return films;
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f JOIN mpa_ratings m ON f.mpa_id = m.id WHERE f.id = ?";
        try {
            Film film = jdbcTemplate.queryForObject(sql, new FilmRowMapper(), id);
            if (film != null) {
                setGenresForFilm(film);
                setLikesForFilm(film);
            }
            return Optional.ofNullable(film);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public Film save(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setObject(3, film.getReleaseDate());
            stmt.setInt(4, film.getDuration());
            stmt.setInt(5, film.getMpa().getId());
            return stmt;
        }, keyHolder);

        Integer filmId = Objects.requireNonNull(keyHolder.getKey()).intValue();
        film.setId(filmId);

        updateFilmGenres(film);
        return film;
    }

    @Override
    public Film update(Film film) {
        findById(film.getId()).orElseThrow(
                () -> new NotFoundException("Фильм с ID " + film.getId() + " не найден")
        );

        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId());

        updateFilmGenres(film);
        return film;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        findById(filmId).orElseThrow(
                () -> new NotFoundException("Фильм с ID " + filmId + " не найден")
        );

        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        findById(filmId).orElseThrow(
                () -> new NotFoundException("Фильм с ID " + filmId + " не найден")
        );

        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        int deleted = jdbcTemplate.update(sql, filmId, userId);

        if (deleted == 0) {
            throw new NotFoundException("Лайк пользователя " + userId + " для фильма " + filmId + " не найден");
        }
    }

    @Override
    public List<Film> findPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(fl.user_id) as likes_count " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name " +
                "ORDER BY likes_count DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, new FilmRowMapper(), count);
        setGenresForFilms(films);
        setLikesForFilms(films);
        return films;
    }

    private void updateFilmGenres(Film film) {
        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());

        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            List<Object[]> batchArgs = film.getGenres().stream()
                    .map(genre -> new Object[]{film.getId(), genre.getId()})
                    .collect(Collectors.toList());
            jdbcTemplate.batchUpdate(insertSql, batchArgs);
        }
    }

    private void setGenresForFilm(Film film) {
        String sql = "SELECT g.id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id = ? " +
                "ORDER BY g.id";

        List<Genre> genres = jdbcTemplate.query(sql, (rs, rowNum) ->
                        new Genre(rs.getInt("id"), rs.getString("name")),
                film.getId()
        );
        film.setGenres(new LinkedHashSet<>(genres));
    }

    private void setGenresForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = String.format("SELECT fg.film_id, g.id, g.name FROM genres g " +
                "JOIN film_genres fg ON g.id = fg.genre_id " +
                "WHERE fg.film_id IN (%s) " +
                "ORDER BY fg.film_id, g.id", placeholders);

        Map<Integer, Set<Genre>> genresByFilmId = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            Integer filmId = rs.getInt("film_id");
            Genre genre = new Genre(rs.getInt("id"), rs.getString("name"));
            genresByFilmId.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
        });

        for (Film film : films) {
            film.setGenres(genresByFilmId.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }

    private void setLikesForFilm(Film film) {
        String sql = "SELECT user_id FROM film_likes WHERE film_id = ?";
        Set<Integer> likes = new HashSet<>(jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("user_id"), film.getId()));
        film.setLikes(likes);
    }

    private void setLikesForFilms(List<Film> films) {
        if (films.isEmpty()) return;

        List<Integer> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
        String placeholders = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = String.format("SELECT film_id, user_id FROM film_likes WHERE film_id IN (%s)", placeholders);
        Map<Integer, Set<Integer>> likesByFilmId = new HashMap<>();

        jdbcTemplate.query(sql, filmIds.toArray(), (rs) -> {
            Integer filmId = rs.getInt("film_id");
            Integer userId = rs.getInt("user_id");
            likesByFilmId.computeIfAbsent(filmId, k -> new HashSet<>()).add(userId);
        });

        for (Film film : films) {
            film.setLikes(likesByFilmId.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    private static class FilmRowMapper implements RowMapper<Film> {
        @Override
        public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
            Film film = new Film();
            film.setId(rs.getInt("id"));
            film.setName(rs.getString("name"));
            film.setDescription(rs.getString("description"));

            Object releaseDateObj = rs.getObject("release_date");
            if (releaseDateObj instanceof java.sql.Date) {
                film.setReleaseDate(((java.sql.Date) releaseDateObj).toLocalDate());
            } else if (releaseDateObj instanceof LocalDate) {
                film.setReleaseDate((LocalDate) releaseDateObj);
            }

            film.setDuration(rs.getInt("duration"));

            Mpa mpa = new Mpa(rs.getInt("mpa_id"), rs.getString("mpa_name"));
            film.setMpa(mpa);

            return film;
        }
    }
}
