package com.javarush.apalinskiy.service;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

public interface UserService {
    User register(Role role, String name, String login, String rawPassword);

    Optional<User> login(String login, String rawPassword);

    Optional<User> findByLogin(String login);

    Optional<User> findById(String userId);

    User updateProfile(String userId, String newDisplayName);

    void changePassword(String userId, String currentPassword, String newPassword);

    List<User> findAll();

    User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull)
            throws DuplicateLoginException, NoSuchElementException, IllegalArgumentException;
}
