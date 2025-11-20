package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({GenreDbStorage.class})
public class GenreDbStorageTest {

    private final GenreDbStorage genreStorage;

    @Test
    public void testGetAllGenres() {
        List<Genre> genres = genreStorage.getAllGenres();

        assertThat(genres).hasSize(6);
        assertThat(genres.get(0)).hasFieldOrPropertyWithValue("id", 1);
        assertThat(genres.get(0)).hasFieldOrPropertyWithValue("name", "Комедия");
    }

    @Test
    public void testGetGenreById() {
        Optional<Genre> genreOptional = genreStorage.getGenreById(1);

        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre ->
                        assertThat(genre).hasFieldOrPropertyWithValue("id", 1)
                                .hasFieldOrPropertyWithValue("name", "Комедия")
                );
    }
}
