package com.javarush.apalinskiy.domain.user;

/**
 * Defines user access roles within the application.
 *
 * <p>The {@code Role} determines a user's authorization level and
 * administrative privileges across the TextQuest platform.</p>
 *
 * <h2>Available roles</h2>
 * <ul>
 *   <li>{@link #USER} — regular player or content creator with standard permissions</li>
 *   <li>{@link #ADMIN} — administrator with elevated privileges such as moderation,
 *       user management, and quest approval</li>
 * </ul>
 */
public enum Role {

    /**
     * Regular user with standard access rights.
     */
    USER,

    /**
     * Administrator with full access and moderation privileges.
     */
    ADMIN
}
