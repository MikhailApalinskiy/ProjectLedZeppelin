package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.repositories.UserRepository;
import com.javarush.apalinskiy.user.Role;
import com.javarush.apalinskiy.user.User;

import java.util.Optional;
import java.util.UUID;

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
}
