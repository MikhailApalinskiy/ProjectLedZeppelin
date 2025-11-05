package com.javarush.apalinskiy.utils;

/**
 * Thread-local storage for the currently authenticated user ID.
 * <p>
 * This class provides a lightweight, per-thread context that holds
 * the identifier of the current user for the duration of a request
 * or operation. It is typically set by a servlet filter or security layer.
 * <p>
 * The context must be cleared after each request to prevent memory leaks
 * in pooled thread environments (e.g., servlet containers).
 */
public final class CurrentUserContext {

    /**
     * Thread-local variable holding the current user ID.
     */
    private static final ThreadLocal<String> TL = new ThreadLocal<>();

    /**
     * Utility class; no instances allowed.
     */
    private CurrentUserContext() {
    }

    /**
     * Sets the current user ID for the active thread.
     *
     * @param userId unique user identifier, may be {@code null}
     */
    public static void set(String userId) {
        TL.set(userId);
    }

    /**
     * Returns the current user ID for the active thread, or {@code null} if not set.
     *
     * @return current user ID, or {@code null} if absent
     */
    public static String get() {
        return TL.get();
    }

    /**
     * Clears the user ID from the current thread context.
     * <p>
     * Must be called at the end of each request to avoid leaking user data
     * between threads in servlet or executor pools.
     */
    public static void clear() {
        TL.remove();
    }

    /**
     * Returns the current user ID, throwing an exception if it is not set or blank.
     *
     * @return non-blank current user ID
     * @throws IllegalStateException if no user ID is present in the context
     */
    public static String require() {
        String id = TL.get();
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("No current user in context");
        }
        return id;
    }
}
