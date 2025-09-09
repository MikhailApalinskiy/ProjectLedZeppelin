package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.application.ports.UserRepository;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.web.util.Web;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

public class DefaultUserService implements UserService {
    private static final int MAX_ID_RETRIES = 3;

    private final UserRepository users;

    public DefaultUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public User register(Role role, String name, String login, String rawPassword) throws DuplicateLoginException {
        User user = User.of(role, name, login, rawPassword);
        for (int i = 0; i < MAX_ID_RETRIES; i++) {
            try {
                users.save(user);
                return user;
            } catch (DuplicateIdException e) {
                user = user.withId(UUID.randomUUID().toString());
            }
        }
        throw new IllegalStateException("Failed to generate unique userId after retries");
    }

    @Override
    public Optional<User> login(String login, String rawPassword) {
        return users.findByLogin(login)
                .filter(u -> u.getPassword().equals(rawPassword));
    }

    @Override
    public Optional<User> findByLogin(String login) {
        return users.findByLogin(login);
    }

    @Override
    public Optional<User> findById(String userId) {
        return users.findById(userId);
    }

    @Override
    public List<User> findAll() {
        return users.findAll();
    }

    @Override
    public User updateProfile(String userId, String newDisplayName) {
        if (StringUtils.isBlank(newDisplayName)) {
            throw new IllegalArgumentException("Display name must not be blank");
        }
        User current = users.findById(userId).orElseThrow(() ->
                new NoSuchElementException("User not found: " + userId));
        User updated = current.withUserName(newDisplayName);
        users.update(updated);
        return updated;
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6) {
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        User current = users.findById(userId).orElseThrow(() ->
                new NoSuchElementException("User not found: " + userId));
        if (!current.getPassword().equals(currentPassword)) {
            throw new SecurityException("Current password is incorrect");
        }
        User updated = current.withPassword(newPassword);
        users.update(updated);
    }

    @Override
    public User changeLogin(String userId, String newLogin) {
        if (StringUtils.isBlank(newLogin)) {
            throw new IllegalArgumentException("Login must not be blank");
        }
        User current = users.findById(userId).orElseThrow(() ->
                new NoSuchElementException("User not found: " + userId));
        User updated = current.withLogin(newLogin);
        users.update(updated);
        return updated;
    }

    @Override
    public User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull) {
        User current = users.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userId));
        String name = Web.trimOrNull(userName);
        String login = Web.trimOrNull(userLogin);
        if (name == null || login == null) {
            throw new IllegalArgumentException("Name and login are required");
        }
        User updated = current
                .withRole(role == null ? current.getRole() : role)
                .withUserName(name)
                .withLogin(login.toLowerCase(Locale.ROOT));
        if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
            if (newPasswordOrNull.equals(current.getPassword())) {
                throw new IllegalArgumentException("New password must differ from current");
            }
            if (newPasswordOrNull.length() < 6) {
                throw new IllegalArgumentException("Password too short");
            }
            updated = updated.withPassword(newPasswordOrNull);
        }
        users.update(updated);
        return updated;
    }
}
