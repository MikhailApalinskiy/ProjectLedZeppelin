package com.javarush.apalinskiy.utils;

/**
 * Functional interface for providing the currently authenticated user’s ID.
 * <p>
 * Typically implemented by web-layer components (e.g., session or security context)
 * to supply the user identifier for service or repository operations.
 */
@FunctionalInterface
public interface CurrentUserProvider {

    /**
     * Returns the unique identifier of the currently authenticated user.
     *
     * @return current user ID (never {@code null})
     */
    String currentUserId();
}
