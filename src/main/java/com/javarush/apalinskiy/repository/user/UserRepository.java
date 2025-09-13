package com.javarush.apalinskiy.repository.user;

import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateIdException;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 * <p>
 * Provides persistence operations for storing, retrieving,
 * and updating user accounts.
 * </p>
 *
 * <h3>Contract</h3>
 * <ul>
 *   <li>Each user must have a unique {@code userId} and {@code userLogin}.</li>
 *   <li>Logins are treated case-insensitively and should be normalized to lowercase.</li>
 *   <li>{@link #save(User)} is used only for new users, and must fail if
 *       the ID or login already exists.</li>
 *   <li>{@link #update(User)} must fail if the user does not already exist,
 *       and must preserve uniqueness of login across users.</li>
 *   <li>{@link #findAll()} should return users in a deterministic order,
 *       typically sorted by creation time and then by login.</li>
 *   <li>Implementations must be thread-safe if accessed concurrently.</li>
 * </ul>
 */
public interface UserRepository {

    /**
     * Finds a user by login.
     * <p>
     * Implementations should normalize the login (trim and lowercase)
     * before lookup.
     * </p>
     *
     * @param userLogin login to search
     * @return optional containing the user if found
     */
    Optional<User> findByLogin(String userLogin);

    /**
     * Finds a user by ID.
     *
     * @param id user ID
     * @return optional containing the user if found
     */
    Optional<User> findById(String id);

    /**
     * Saves a new user.
     * <p>
     * Must enforce uniqueness of {@code userId} and {@code userLogin}.
     * </p>
     *
     * @param user user to persist
     * @throws DuplicateLoginException if login already exists
     * @throws DuplicateIdException    if userId already exists
     */
    void save(User user);

    /**
     * Updates an existing user.
     * <p>
     * Must throw an exception if the user does not exist.
     * If login changes, must enforce uniqueness.
     * </p>
     *
     * @param user user with updated fields
     * @throws NoSuchElementException  if user not found
     * @throws DuplicateLoginException if new login already exists
     */
    void update(User user);

    /**
     * Returns all users in deterministic order.
     *
     * @return list of users (never {@code null}, may be empty)
     */
    List<User> findAll();
}
