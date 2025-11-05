package com.javarush.apalinskiy.exceptions;

/**
 * Exception thrown when attempting to register or create a user
 * with a login that already exists in the system.
 *
 * <p>This exception is typically raised during user registration
 * or credential updates to enforce unique {@code userLogin} values
 * within the {@code users} table.</p>
 */
public class DuplicateLoginException extends RuntimeException {

    /**
     * Constructs a new {@code DuplicateLoginException} with a specified message.
     *
     * @param message description of the duplication conflict
     */
    public DuplicateLoginException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code DuplicateLoginException} with no detail message.
     */
    public DuplicateLoginException() {
    }
}
