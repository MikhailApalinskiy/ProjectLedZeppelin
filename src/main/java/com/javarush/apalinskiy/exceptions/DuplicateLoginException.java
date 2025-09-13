package com.javarush.apalinskiy.exceptions;

/**
 * Exception thrown when attempting to register a user with a login
 * that already exists in the system.
 * <p>
 * Typically used by {@code UserService} or repository classes
 * to enforce unique logins.
 * </p>
 *
 * <p>This is an unchecked exception (subclass of {@link RuntimeException}).</p>
 */
public class DuplicateLoginException extends RuntimeException {

    /**
     * Creates a new {@code DuplicateLoginException} with a custom message.
     *
     * @param message description of the error
     */
    public DuplicateLoginException(String message) {
        super(message);
    }


    /**
     * Creates a new {@code DuplicateLoginException} with no detail message.
     */
    public DuplicateLoginException() {
    }
}
