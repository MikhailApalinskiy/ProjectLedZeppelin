package com.javarush.apalinskiy.repository.user;

import com.javarush.apalinskiy.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String userLogin);

    Optional<User> findById(String id);

    void save(User user);

    void update(User user);

    List<User> findAll();
}
