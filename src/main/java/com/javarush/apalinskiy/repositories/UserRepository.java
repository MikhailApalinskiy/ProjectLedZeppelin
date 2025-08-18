package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.user.User;

import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String userLogin);
    Optional<User> findById(long id);
    void save(User user);
}
