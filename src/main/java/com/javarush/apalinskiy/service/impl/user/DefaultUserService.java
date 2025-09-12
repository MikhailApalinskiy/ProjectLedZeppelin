package com.javarush.apalinskiy.service.impl.user;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repository.user.UserRepository;
import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.service.user.UserService;
import com.javarush.apalinskiy.web.util.Web;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultUserService implements UserService {

    private static final Logger log = LoggerFactory.getLogger(DefaultUserService.class);
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
                log.info("User registered id={} login='{}' role={}", user.getUserId(), user.getUserLogin(), role);
                return user;
            } catch (DuplicateIdException e) {
                log.warn("Duplicate userId={} on register, retrying...", user.getUserId());
                user = user.withId(UUID.randomUUID().toString());
            }
        }
        log.error("Failed to generate unique userId for login='{}' after {} retries", login, MAX_ID_RETRIES);
        throw new IllegalStateException("Failed to generate unique userId after retries");
    }

    @Override
    public Optional<User> login(String login, String rawPassword) {
        Optional<User> res = users.findByLogin(login)
                .filter(u -> u.getPassword().equals(rawPassword));
        if (res.isPresent()) {
            log.info("User login success login='{}' id={}", login, res.get().getUserId());
        } else {
            log.warn("User login failed login='{}'", login);
        }
        return res;
    }

    @Override
    public Optional<User> findByLogin(String login) {
        Optional<User> res = users.findByLogin(login);
        log.debug("findByLogin login='{}' found={}", login, res.isPresent());
        return res;
    }

    @Override
    public Optional<User> findById(String userId) {
        Optional<User> res = users.findById(userId);
        log.debug("findById id={} found={}", userId, res.isPresent());
        return res;
    }

    @Override
    public List<User> findAll() {
        List<User> list = users.findAll();
        log.debug("findAll size={}", list.size());
        return list;
    }

    @Override
    public User updateProfile(String userId, String newDisplayName) {
        if (StringUtils.isBlank(newDisplayName)) {
            log.warn("updateProfile denied: blank displayName userId={}", userId);
            throw new IllegalArgumentException("Display name must not be blank");
        }
        User current = users.findById(userId).orElseThrow(() -> {
            log.warn("updateProfile denied: user not found id={}", userId);
            return new NoSuchElementException("User not found: " + userId);
        });
        User updated = current.withUserName(newDisplayName);
        users.update(updated);
        log.info("User profile updated id={} newDisplayName='{}'", userId, newDisplayName);
        return updated;
    }

    @Override
    public void changePassword(String userId, String currentPassword, String newPassword) {
        if (StringUtils.isBlank(newPassword) || newPassword.length() < 6) {
            log.warn("changePassword denied: invalid new password userId={}", userId);
            throw new IllegalArgumentException("New password must be at least 6 characters");
        }
        User current = users.findById(userId).orElseThrow(() -> {
            log.warn("changePassword denied: user not found id={}", userId);
            return new NoSuchElementException("User not found: " + userId);
        });
        if (!current.getPassword().equals(currentPassword)) {
            log.warn("changePassword denied: incorrect current password userId={}", userId);
            throw new SecurityException("Current password is incorrect");
        }
        User updated = current.withPassword(newPassword);
        users.update(updated);
        log.info("User password changed id={}", userId);
    }

    @Override
    public User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull) {
        User current = users.findById(userId)
                .orElseThrow(() -> {
                    log.warn("adminUpdate denied: user not found id={}", userId);
                    return new NoSuchElementException("User not found: " + userId);
                });
        String name = Web.trimOrNull(userName);
        String login = Web.trimOrNull(userLogin);
        if (name == null || login == null) {
            log.warn("adminUpdate denied: blank name/login userId={}", userId);
            throw new IllegalArgumentException("Name and login are required");
        }
        User updated = current
                .withRole(role == null ? current.getRole() : role)
                .withUserName(name)
                .withLogin(login.toLowerCase(Locale.ROOT));
        if (newPasswordOrNull != null && !newPasswordOrNull.isBlank()) {
            if (newPasswordOrNull.equals(current.getPassword())) {
                log.warn("adminUpdate denied: new password same as current userId={}", userId);
                throw new IllegalArgumentException("New password must differ from current");
            }
            if (newPasswordOrNull.length() < 6) {
                log.warn("adminUpdate denied: new password too short userId={}", userId);
                throw new IllegalArgumentException("Password too short");
            }
            updated = updated.withPassword(newPasswordOrNull);
        }
        users.update(updated);
        log.info("User updated by admin id={} role={} login='{}' name='{}'", userId, updated.getRole(), updated.getUserLogin(), updated.getUserName());
        return updated;
    }
}
