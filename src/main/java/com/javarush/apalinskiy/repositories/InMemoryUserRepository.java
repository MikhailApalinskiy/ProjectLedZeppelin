package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.user.User;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<String, User> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> idByLogin = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByLogin(String userLogin) {
        final String key = userLogin.trim().toLowerCase(Locale.ROOT);
        String id = idByLogin.get(key);
        return (id == null) ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void save(User user) {
        final String id = user.getUserId();
        final String login = user.getUserLogin();
        synchronized (this) {
            if (idByLogin.containsKey(login)) {
                throw new DuplicateLoginException("Login already exists: " + login);
            }
            if (byId.containsKey(id)) {
                throw new DuplicateIdException("UserId already exists: " + id);
            }
            idByLogin.put(login, id);
            byId.put(id, user);
        }
    }
}