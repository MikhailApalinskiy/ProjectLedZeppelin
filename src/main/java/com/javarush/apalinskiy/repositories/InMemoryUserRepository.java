package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.user.User;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<Long, User> byId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> idByLogin = new ConcurrentHashMap<>();

    @Override
    public Optional<User> findByLogin(String userLogin) {
        Long id = idByLogin.get(userLogin);
        return id == null ? Optional.empty() : Optional.ofNullable(byId.get(id));
    }

    @Override
    public Optional<User> findById(long id) {
        return Optional.ofNullable(byId.get(id));
    }

    @Override
    public void save(User user) {
        String key = user.getUserLogin();
        Long prev = idByLogin.putIfAbsent(key, user.getUserId());
        if (prev != null) {
            throw new IllegalStateException("Login already exists: " + key);
        }
        byId.put(user.getUserId(), user);
    }
}
