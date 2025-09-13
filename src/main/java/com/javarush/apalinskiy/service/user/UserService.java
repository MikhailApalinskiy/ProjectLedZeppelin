package com.javarush.apalinskiy.service.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service for managing users, authentication, and profile operations.
 * <p>
 * Provides methods for registering, logging in, finding, updating, and
 * administratively managing user accounts.
 * </p>
 *
 * <h3>Responsibilities</h3>
 * <ul>
 *   <li>User registration and authentication.</li>
 *   <li>Profile management (display name, password).</li>
 *   <li>Administrative updates (role, login, password reset).</li>
 *   <li>Lookup of users by login or ID.</li>
 * </ul>
 */
public interface UserService {

    /**
     * Registers a new user with the given details.
     *
     * @param role        role to assign (defaults to {@link Role#USER} if {@code null})
     * @param name        display name
     * @param login       unique login identifier
     * @param rawPassword plaintext password
     * @return the created {@link User}
     * @throws DuplicateLoginException  if a user with the same login already exists
     * @throws IllegalArgumentException if required fields are missing
     */
    User register(Role role, String name, String login, String rawPassword);

    /**
     * Attempts to log in a user with the provided credentials.
     *
     * @param login       login identifier
     * @param rawPassword plaintext password
     * @return an {@link Optional} containing the authenticated user, or empty if authentication fails
     */
    Optional<User> login(String login, String rawPassword);

    /**
     * Finds a user by their login.
     *
     * @param login login identifier
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findByLogin(String login);

    /**
     * Finds a user by their ID.
     *
     * @param userId unique user identifier
     * @return an {@link Optional} containing the user if found
     */
    Optional<User> findById(String userId);

    /**
     * Updates the display name of a user.
     *
     * @param userId         user identifier
     * @param newDisplayName new display name (must not be blank)
     * @return the updated {@link User}
     * @throws NoSuchElementException   if the user does not exist
     * @throws IllegalArgumentException if the new display name is invalid
     */
    User updateProfile(String userId, String newDisplayName);

    /**
     * Changes the password of a user.
     *
     * @param userId          user identifier
     * @param currentPassword current password
     * @param newPassword     new password (must be at least 6 characters and different from current)
     * @throws NoSuchElementException   if the user does not exist
     * @throws SecurityException        if the current password is incorrect
     * @throws IllegalArgumentException if the new password is invalid
     */
    void changePassword(String userId, String currentPassword, String newPassword);

    /**
     * Returns all registered users.
     *
     * @return list of all users, never {@code null}
     */
    List<User> findAll();

    /**
     * Updates a user profile with administrative privileges.
     * <p>
     * Allows changing role, login, display name, and optionally resetting the password.
     * </p>
     *
     * @param userId            user identifier
     * @param role              new role (or {@code null} to keep current)
     * @param userName          new display name
     * @param userLogin         new login (must be unique)
     * @param newPasswordOrNull new password or {@code null} to keep current
     * @return the updated {@link User}
     * @throws DuplicateLoginException  if the new login is already taken
     * @throws NoSuchElementException   if the user does not exist
     * @throws IllegalArgumentException if the provided values are invalid
     */
    User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull)
            throws DuplicateLoginException, NoSuchElementException, IllegalArgumentException;
}
