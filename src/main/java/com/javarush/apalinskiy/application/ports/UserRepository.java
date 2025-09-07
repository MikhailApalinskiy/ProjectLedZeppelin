package com.javarush.apalinskiy.application.ports;

import com.javarush.apalinskiy.domain.user.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String userLogin);

    Optional<User> findById(String id);

    void save(User user);
}
