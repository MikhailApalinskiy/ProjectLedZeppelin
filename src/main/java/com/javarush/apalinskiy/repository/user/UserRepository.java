package com.javarush.apalinskiy.repository.user;

import com.javarush.apalinskiy.domain.user.User;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing {@link User} entities.
 *
 * <p>This interface defines persistence operations related to user accounts,
 * including creation, update, lookup, and pagination. It is implemented by
 * Hibernate-based classes such as {@code HUserRepository} that handle entity
 * management and constraint enforcement (e.g. duplicate login or ID).</p>
 *
 * <p>Each {@link User} entity represents an application account with a unique
 * login and a generated identifier. Uniqueness constraints are typically enforced
 * at both the database and application level through exceptions like
 * {@code DuplicateLoginException} and {@code DuplicateIdException}.</p>
 */
public interface UserRepository {

    /**
     * Returns a paginated list of users ordered by creation date (descending)
     * and login (ascending).
     *
     * @param page page number (1-based)
     * @param size number of records per page
     * @return immutable list of users for the requested page
     */
    List<User> findPage(int page, int size);

    /**
     * Retrieves a user by their unique login name.
     *
     * @param userLogin login string (case-insensitive)
     * @return optional containing the found user, or empty if not found
     */
    Optional<User> findByLogin(String userLogin);

    /**
     * Retrieves a user by their unique identifier.
     *
     * @param id user ID (UUID string)
     * @return optional containing the found user, or empty if not found
     */
    Optional<User> findById(String id);

    /**
     * Persists a new user to the database.
     *
     * <p>If a user with the same login or ID already exists, an exception is thrown:
     * <ul>
     *   <li>{@code DuplicateLoginException} — when the login already exists</li>
     *   <li>{@code DuplicateIdException} — when the user ID already exists</li>
     * </ul>
     * </p>
     *
     * @param user new {@link User} entity to persist
     * @throws com.javarush.apalinskiy.exceptions.DuplicateLoginException if login is not unique
     * @throws com.javarush.apalinskiy.exceptions.DuplicateIdException    if user ID is not unique
     */
    void save(User user);

    /**
     * Updates an existing user in the database.
     *
     * <p>Attempts to merge the entity state with the current persistence context.
     * If a login conflict occurs, a {@code DuplicateLoginException} is thrown.</p>
     *
     * @param user {@link User} entity with updated fields
     * @throws com.javarush.apalinskiy.exceptions.DuplicateLoginException if new login conflicts with another user
     */
    void update(User user);

    /**
     * Counts the total number of registered users.
     *
     * @return total user count in the system
     */
    long countAll();
}
