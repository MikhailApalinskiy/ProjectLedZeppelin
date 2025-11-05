package com.javarush.apalinskiy.service.user;

import com.javarush.apalinskiy.domain.user.Role;
import com.javarush.apalinskiy.domain.user.User;
import com.javarush.apalinskiy.exceptions.DuplicateLoginException;
import com.javarush.apalinskiy.service.impl.user.DefaultUserService;

import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Service interface for managing user registration, authentication, and profile updates.
 *
 * <p>This interface defines the main user management operations, including user creation,
 * login validation, password management, and administrative updates. Implementations
 * are responsible for enforcing business rules, validation, and security checks.</p>
 */
public interface UserService {

    /**
     * Registers a new user with the given role and credentials.
     *
     * @param role        user role (e.g., PLAYER, ADMIN)
     * @param name        display name
     * @param login       unique login name
     * @param rawPassword plain text password (will be hashed)
     * @return newly registered {@link User}
     * @throws DuplicateLoginException if a user with the same login already exists
     */
    User register(Role role, String name, String login, String rawPassword);

    /**
     * Attempts to log in a user using their credentials.
     *
     * @param login       user login
     * @param rawPassword plain text password for verification
     * @return optional user if authentication succeeds; otherwise empty
     */
    Optional<User> login(String login, String rawPassword);

    /**
     * Finds a user by their login name.
     *
     * @param login user login
     * @return optional user if found; otherwise empty
     */
    Optional<User> findByLogin(String login);

    /**
     * Finds a user by their unique identifier.
     *
     * @param userId user ID
     * @return optional user if found; otherwise empty
     */
    Optional<User> findById(String userId);

    /**
     * Updates the display name of an existing user.
     *
     * @param userId         user ID
     * @param newDisplayName new display name to set
     * @return updated {@link User}
     * @throws NoSuchElementException if the user does not exist
     */
    User updateProfile(String userId, String newDisplayName);

    /**
     * Changes the password of an existing user.
     *
     * <p>The method validates the current password before applying the change.</p>
     *
     * @param userId          user ID
     * @param currentPassword current plain text password
     * @param newPassword     new plain text password
     * @throws IllegalArgumentException if validation fails
     */
    void changePassword(String userId, String currentPassword, String newPassword);

    /**
     * Returns a paginated list of users for administrative views.
     *
     * @param page page number (1-based)
     * @param size number of users per page
     * @return {@link DefaultUserService.PagedResult} containing users and pagination info
     */
    DefaultUserService.PagedResult<User> findPage(int page, int size);

    /**
     * Performs an administrative update on a user account.
     *
     * <p>This method allows changing role, name, login, and optionally password.
     * It performs validation and prevents duplicate logins.</p>
     *
     * @param userId            user identifier
     * @param role              new role
     * @param userName          new display name
     * @param userLogin         new login name
     * @param newPasswordOrNull optional new password; {@code null} to leave unchanged
     * @return updated {@link User}
     * @throws DuplicateLoginException  if the new login already exists
     * @throws NoSuchElementException   if the user is not found
     * @throws IllegalArgumentException if invalid parameters are provided
     */
    User adminUpdate(String userId, Role role, String userName, String userLogin, String newPasswordOrNull)
            throws DuplicateLoginException, NoSuchElementException, IllegalArgumentException;
}
