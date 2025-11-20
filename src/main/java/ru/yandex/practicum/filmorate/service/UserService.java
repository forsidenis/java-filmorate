package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public List<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Integer id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с ID " + id + " не найден"));
    }

    public User create(User user) {
        validateUser(user);
        processUserName(user);

        try {
            return userStorage.save(user);
        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка целостности данных при создании пользователя: {}", e.getMessage());
            throw new ValidationException("Пользователь с таким email или логином уже существует");
        }
    }

    public User update(User user) {
        validateUser(user);
        processUserName(user);

        // Проверяем, что пользователь существует
        findById(user.getId());

        try {
            return userStorage.update(user);
        } catch (DataIntegrityViolationException e) {
            log.error("Ошибка целостности данных при обновлении пользователя: {}", e.getMessage());
            throw new ValidationException("Пользователь с таким email или логином уже существует");
        }
    }

    public void addFriend(Integer userId, Integer friendId) {
        if (userId.equals(friendId)) {
            throw new ValidationException("Пользователь не может добавить сам себя в друзья");
        }

        // Проверяем существование обоих пользователей
        findById(userId);
        findById(friendId);

        userStorage.addFriend(userId, friendId);
    }

    public void removeFriend(Integer userId, Integer friendId) {
        // Проверяем существование обоих пользователей
        findById(userId);
        findById(friendId);

        userStorage.removeFriend(userId, friendId);
    }

    public List<User> findFriends(Integer userId) {
        // Проверяем существование пользователя
        findById(userId);

        return userStorage.findFriends(userId);
    }

    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        // Проверяем существование обоих пользователей
        findById(userId);
        findById(otherId);

        return userStorage.findCommonFriends(userId, otherId);
    }

    private void processUserName(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
            log.debug("Имя пользователя не указано, используется логин: {}", user.getLogin());
        }
    }

    private void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Email не может быть пустым");
        }
        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Email должен содержать @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым");
        }
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может содержать пробелы");
        }
        if (user.getBirthday() == null) {
            throw new ValidationException("Дата рождения не может быть пустой");
        }
        if (user.getBirthday().isAfter(java.time.LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }
}
