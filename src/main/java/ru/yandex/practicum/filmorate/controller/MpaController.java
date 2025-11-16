package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mpa.MpaStorage;

import java.util.List;

@RestController
@RequestMapping("/mpa")
@RequiredArgsConstructor
public class MpaController {
    private final MpaStorage mpaStorage;

    @GetMapping
    public List<Mpa> getAllMpa() {
        return mpaStorage.findAll();
    }

    @GetMapping("/{id}")
    public Mpa getMpa(@PathVariable Integer id) {
        return mpaStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Рейтинг MPA с ID " + id + " не найден"));
    }
}
