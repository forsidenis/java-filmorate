package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({MpaDbStorage.class})
public class MpaDbStorageTest {

    private final MpaDbStorage mpaStorage;

    @Test
    public void testGetAllMpa() {
        List<Mpa> mpaList = mpaStorage.getAllMpa();

        assertThat(mpaList).hasSize(5);
        assertThat(mpaList.get(0)).hasFieldOrPropertyWithValue("id", 1);
    }

    @Test
    public void testGetMpaById() {
        Optional<Mpa> mpaOptional = mpaStorage.getMpaById(1);

        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa ->
                        assertThat(mpa).hasFieldOrPropertyWithValue("id", 1)
                );
    }
}
