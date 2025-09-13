package com.javarush.apalinskiy.domain.user;

/**
 * Enumeration of application user roles.
 * <p>
 * Roles define the level of access a user has within the system.
 * </p>
 *
 * <ul>
 *   <li>{@link #USER} – regular user with standard permissions
 *       (e.g. playing quests, managing own profile, sending friend requests).</li>
 *   <li>{@link #ADMIN} – administrator with elevated privileges
 *       (e.g. moderating quests, editing users, managing system settings).</li>
 * </ul>
 */
public enum Role {
    /**
     * Regular user with standard access rights.
     */
    USER,
    /**
     * Administrator with elevated privileges.
     */
    ADMIN
}
