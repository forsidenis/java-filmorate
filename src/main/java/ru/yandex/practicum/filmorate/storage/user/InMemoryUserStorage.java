package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.exception.NotFoundException;

import java.util.*;
import java.util.stream.Collectors;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {
    private final Map<Integer, User> users = new HashMap<>();
    private final Map<Integer, Set<Integer>> friendships = new HashMap<>();
    private int nextId = 1;

    @Override
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    @Override
    public Optional<User> findById(Integer id) {
        return Optional.ofNullable(users.get(id));
    }

    @Override
    public User save(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        friendships.put(user.getId(), new HashSet<>());
        log.info("Создан пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User update(User user) {
        if (!users.containsKey(user.getId())) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }
        users.put(user.getId(), user);
        log.info("Обновлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public void delete(Integer id) {
        users.remove(id);
        friendships.remove(id);
        // Удаляем из друзей у всех пользователей
        friendships.values().forEach(friends -> friends.remove(id));
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        validateUserExists(userId);
        validateUserExists(friendId);

        friendships.get(userId).add(friendId);
        friendships.get(friendId).add(userId);
        log.info("Пользователи {} и {} теперь друзья", userId, friendId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        validateUserExists(userId);
        validateUserExists(friendId);

        friendships.get(userId).remove(friendId);
        friendships.get(friendId).remove(userId);
        log.info("Пользователи {} и {} больше не друзья", userId, friendId);
    }

    @Override
    public List<User> findFriends(Integer userId) {
        validateUserExists(userId);
        return friendships.get(userId).stream()
                .map(users::get)
                .collect(Collectors.toList());
    }

    @Override
    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        validateUserExists(userId);
        validateUserExists(otherId);

        Set<Integer> userFriends = friendships.get(userId);
        Set<Integer> otherFriends = friendships.get(otherId);

        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(users::get)
                .collect(Collectors.toList());
    }

    private void validateUserExists(Integer userId) {
        if (!users.containsKey(userId)) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
    }
}
