package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Primary
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, this::mapRowToUser);

        // Загружаем друзей для всех пользователей одним запросом
        if (!users.isEmpty()) {
            loadFriendsForUsers(users);
        }

        return users;
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        List<User> results = jdbcTemplate.query(sql, this::mapRowToUser, id);

        if (results.isEmpty()) {
            return Optional.empty();
        }

        User user = results.get(0);
        // Загружаем друзей для конкретного пользователя
        user.setFriends(getUserFriends(user.getId()));

        return Optional.of(user);
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, new String[]{"id"});
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setDate(4, Date.valueOf(user.getBirthday()));
            return stmt;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return user;
    }

    @Override
    public User update(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE id = ?";
        jdbcTemplate.update(sql,
                user.getEmail(),
                user.getLogin(),
                user.getName(),
                user.getBirthday(),
                user.getId());
        return user;
    }

    @Override
    public void delete(Integer id) {
        String sql = "DELETE FROM users WHERE id = ?";
        jdbcTemplate.update(sql, id);
    }

    @Override
    public void addFriend(Integer userId, Integer friendId) {
        String sql = "INSERT INTO friends (user_id, friend_id, confirmed) VALUES (?, ?, false)";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        String sql = "DELETE FROM friends WHERE user_id = ? AND friend_id = ?";
        jdbcTemplate.update(sql, userId, friendId);
    }

    @Override
    public List<User> findFriends(Integer userId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";

        List<User> friends = jdbcTemplate.query(sql, this::mapRowToUser, userId);

        // Загружаем друзей для каждого пользователя в списке
        if (!friends.isEmpty()) {
            loadFriendsForUsers(friends);
        }

        return friends;
    }

    @Override
    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        String sql = "SELECT u.* FROM users u " +
                "JOIN friends f1 ON u.id = f1.friend_id " +
                "JOIN friends f2 ON u.id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";

        List<User> commonFriends = jdbcTemplate.query(sql, this::mapRowToUser, userId, otherId);

        // Загружаем друзей для каждого пользователя в списке
        if (!commonFriends.isEmpty()) {
            loadFriendsForUsers(commonFriends);
        }

        return commonFriends;
    }

    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());

        // Инициализируем пустой Set для друзей
        user.setFriends(new HashSet<>());

        return user;
    }

    /**
     * Получает множество ID друзей пользователя
     */
    private Set<Integer> getUserFriends(Integer userId) {
        String sql = "SELECT friend_id FROM friends WHERE user_id = ?";
        List<Integer> friendsList = jdbcTemplate.queryForList(sql, Integer.class, userId);
        return new HashSet<>(friendsList);
    }

    /**
     * Оптимизированная загрузка друзей для списка пользователей одним запросом
     */
    private void loadFriendsForUsers(List<User> users) {
        List<Integer> userIds = users.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        if (userIds.isEmpty()) {
            return;
        }

        // Создаем IN clause с нужным количеством параметров
        String inClause = String.join(",", Collections.nCopies(userIds.size(), "?"));

        String sql = String.format(
                "SELECT user_id, friend_id FROM friends WHERE user_id IN (%s)",
                inClause
        );

        // Выполняем запрос и маппим результаты
        Map<Integer, Set<Integer>> friendsByUserId = jdbcTemplate.query(
                sql,
                userIds.toArray(),
                rs -> {
                    Map<Integer, Set<Integer>> result = new HashMap<>();
                    while (rs.next()) {
                        Integer userId = rs.getInt("user_id");
                        Integer friendId = rs.getInt("friend_id");
                        result.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
                    }
                    return result;
                }
        );

        // Устанавливаем друзей для каждого пользователя
        users.forEach(user -> {
            Set<Integer> friends = friendsByUserId.getOrDefault(user.getId(), new HashSet<>());
            user.setFriends(friends);
        });
    }
}
