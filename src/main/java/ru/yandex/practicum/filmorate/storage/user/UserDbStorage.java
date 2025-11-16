package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

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
@Slf4j
public class UserDbStorage implements UserStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = jdbcTemplate.query(sql, new UserRowMapper());
        setFriendsForUsers(users);
        return users;
    }

    @Override
    public Optional<User> findById(Integer id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try {
            User user = jdbcTemplate.queryForObject(sql, new UserRowMapper(), id);
            if (user != null) {
                setFriendsForUser(user);
            }
            return Optional.ofNullable(user);
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    @Override
    public User save(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            stmt.setString(1, user.getEmail());
            stmt.setString(2, user.getLogin());
            stmt.setString(3, user.getName());
            stmt.setObject(4, user.getBirthday()); // Используем setObject
            return stmt;
        }, keyHolder);

        user.setId(Objects.requireNonNull(keyHolder.getKey()).intValue());
        return user;
    }

    @Override
    public User update(User user) {
        findById(user.getId()).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + user.getId() + " не найден")
        );

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
        findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + userId + " не найден")
        );
        findById(friendId).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + friendId + " не найден")
        );

        String sql = "INSERT INTO friendships (user_id, friend_id) VALUES (?, ?)";
        try {
            jdbcTemplate.update(sql, userId, friendId);
            log.debug("Пользователь {} добавил в друзья пользователя {}", userId, friendId);
        } catch (DataIntegrityViolationException e) {
            log.debug("Дружба между пользователями {} и {} уже существует", userId, friendId);
        }
    }

    @Override
    public void removeFriend(Integer userId, Integer friendId) {
        String sql = "DELETE FROM friendships WHERE user_id = ? AND friend_id = ?";
        int deleted = jdbcTemplate.update(sql, userId, friendId);

        if (deleted == 0) {
            throw new NotFoundException("Дружба между пользователями " + userId + " и " + friendId + " не найдена");
        }
    }

    @Override
    public List<User> findFriends(Integer userId) {
        findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + userId + " не найден")
        );

        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f ON u.id = f.friend_id " +
                "WHERE f.user_id = ?";

        List<User> friends = jdbcTemplate.query(sql, new UserRowMapper(), userId);
        friends.forEach(this::setFriendsForUser);
        return friends;
    }

    @Override
    public List<User> findCommonFriends(Integer userId, Integer otherId) {
        findById(userId).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + userId + " не найден")
        );
        findById(otherId).orElseThrow(
                () -> new NotFoundException("Пользователь с ID " + otherId + " не найден")
        );

        String sql = "SELECT u.* FROM users u " +
                "JOIN friendships f1 ON u.id = f1.friend_id " +
                "JOIN friendships f2 ON u.id = f2.friend_id " +
                "WHERE f1.user_id = ? AND f2.user_id = ?";

        List<User> commonFriends = jdbcTemplate.query(sql, new UserRowMapper(), userId, otherId);
        commonFriends.forEach(this::setFriendsForUser);
        return commonFriends;
    }

    private void setFriendsForUser(User user) {
        String sql = "SELECT friend_id FROM friendships WHERE user_id = ?";
        Set<Integer> friends = new HashSet<>(jdbcTemplate.query(sql,
                (rs, rowNum) -> rs.getInt("friend_id"),
                user.getId()
        ));
        user.setFriends(friends);
    }

    private void setFriendsForUsers(List<User> users) {
        if (users.isEmpty()) return;

        List<Integer> userIds = users.stream().map(User::getId).collect(Collectors.toList());
        String placeholders = String.join(",", Collections.nCopies(userIds.size(), "?"));

        String sql = String.format("SELECT user_id, friend_id FROM friendships WHERE user_id IN (%s)", placeholders);
        Map<Integer, Set<Integer>> friendsByUserId = new HashMap<>();

        jdbcTemplate.query(sql, userIds.toArray(), (rs) -> {
            Integer userId = rs.getInt("user_id");
            Integer friendId = rs.getInt("friend_id");
            friendsByUserId.computeIfAbsent(userId, k -> new HashSet<>()).add(friendId);
        });

        for (User user : users) {
            user.setFriends(friendsByUserId.getOrDefault(user.getId(), new HashSet<>()));
        }
    }

    private static class UserRowMapper implements RowMapper<User> {
        @Override
        public User mapRow(ResultSet rs, int rowNum) throws SQLException {
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setEmail(rs.getString("email"));
            user.setLogin(rs.getString("login"));
            user.setName(rs.getString("name"));

            Object birthdayObj = rs.getObject("birthday");
            if (birthdayObj instanceof java.sql.Date) {
                user.setBirthday(((java.sql.Date) birthdayObj).toLocalDate());
            } else if (birthdayObj instanceof LocalDate) {
                user.setBirthday((LocalDate) birthdayObj);
            }

            return user;
        }
    }
}
