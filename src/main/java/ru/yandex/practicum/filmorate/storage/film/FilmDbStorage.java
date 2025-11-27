package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Film> findAll() {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f JOIN mpa_ratings m ON f.mpa_id = m.id";
        return jdbcTemplate.query(sql, this::mapRowToFilm);
    }

    @Override
    public Optional<Film> findById(Integer id) {
        String sql = "SELECT f.*, m.name as mpa_name FROM films f JOIN mpa_ratings m ON f.mpa_id = m.id WHERE f.id = ?";
        List<Film> results = jdbcTemplate.query(sql, this::mapRowToFilm, id);
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public Film save(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_id) VALUES (?, ?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, film.getName());
            stmt.setString(2, film.getDescription());
            stmt.setDate(3, Date.valueOf(film.getReleaseDate()));
            stmt.setInt(4, film.getDuration());
            stmt.setInt(5, film.getMpa().getId());
            return stmt;
        }, keyHolder);

        film.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());

        // Обновляем жанры только если они есть
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            updateFilmGenres(film);
        }

        // Загружаем полную информацию о MPA
        film.setMpa(getMpaById(film.getMpa().getId()));

        return film;
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                film.getReleaseDate(),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );

        if (updated == 0) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        // Обновляем жанры
        updateFilmGenres(film);

        // Загружаем полную информацию о MPA
        film.setMpa(getMpaById(film.getMpa().getId()));

        return film;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM films WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void addLike(Integer filmId, Integer userId) {
        String sql = "INSERT INTO film_likes (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public void removeLike(Integer filmId, Integer userId) {
        String sql = "DELETE FROM film_likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> findPopularFilms(int count) {
        String sql = "SELECT f.*, m.name as mpa_name, COUNT(fl.user_id) as likes_count " +
                "FROM films f " +
                "JOIN mpa_ratings m ON f.mpa_id = m.id " +
                "LEFT JOIN film_likes fl ON f.id = fl.film_id " +
                "GROUP BY f.id, m.name " +
                "ORDER BY COUNT(fl.user_id) DESC " +
                "LIMIT ?";

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, count);

        // Загружаем жанры для каждого фильма
        if (!films.isEmpty()) {
            loadGenresForFilms(films);
        }

        return films;
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        Film film = new Film();
        film.setId(rs.getInt("id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));

        Mpa mpa = new Mpa();
        mpa.setId(rs.getInt("mpa_id"));
        mpa.setName(rs.getString("mpa_name"));
        film.setMpa(mpa);

        // Получаем количество лайков из того же запроса, если есть
        try {
            film.setRate(rs.getInt("likes_count"));
        } catch (SQLException e) {
            // Если колонки likes_count нет, получаем отдельным запросом
            film.setRate(getLikesCount(film.getId()));
        }

        return film;
    }

    private Integer getLikesCount(Integer filmId) {
        String sql = "SELECT COUNT(*) FROM film_likes WHERE film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, filmId);
        return count != null ? count : 0;
    }

    private Mpa getMpaById(Integer mpaId) {
        String sql = "SELECT * FROM mpa_ratings WHERE id = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            Mpa mpa = new Mpa();
            mpa.setId(rs.getInt("id"));
            mpa.setName(rs.getString("name"));
            return mpa;
        }, mpaId);
    }

    private void updateFilmGenres(Film film) {
        // Удаляем старые жанры
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());

        // Добавляем новые жанры (если они есть и не null)
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            // Убираем дубликаты жанров
            Set<Genre> uniqueGenres = film.getGenres().stream()
                    .filter(genre -> genre != null && genre.getId() != null)
                    .collect(Collectors.toCollection(() ->
                            new TreeSet<>(Comparator.comparing(Genre::getId))));

            String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
            for (Genre genre : uniqueGenres) {
                jdbcTemplate.update(insertSql, film.getId(), genre.getId());
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
        String inClause = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = String.format(
                "SELECT fg.film_id, g.id, g.name " +
                        "FROM film_genres fg " +
                        "JOIN genres g ON fg.genre_id = g.id " +
                        "WHERE fg.film_id IN (%s) " +
                        "ORDER BY fg.film_id, g.id", inClause
        );
        // Выполняем запрос и маппим результаты
        Map<Integer, List<Genre>> genresByFilmId = jdbcTemplate.query(
                sql,
                filmIds.toArray(),
                rs -> {
                    Map<Integer, List<Genre>> result = new HashMap<>();
                    while (rs.next()) {
                        Integer filmId = rs.getInt("film_id");
                        Genre genre = new Genre(rs.getInt("id"), rs.getString("name"));
                        result.computeIfAbsent(filmId, k -> new ArrayList<>()).add(genre);
                    }
                    return result;
                }
        );

        // Устанавливаем жанры для каждого фильма
        films.forEach(film -> {
            List<Genre> genres = genresByFilmId.getOrDefault(film.getId(), new ArrayList<>());
            film.setGenres(genres);
        });
    }
}
