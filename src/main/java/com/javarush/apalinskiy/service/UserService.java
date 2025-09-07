package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;

import java.util.Optional;

public interface UserService {
    User register(Role role, String name, String login, String rawPassword);

    Optional<User> login(String login, String rawPassword);

    Optional<User> findByLogin(String login);
}
