package com.javarush.apalinskiy.exceptions;

/**
 * Exception thrown when a duplicate identifier is detected.
 * <p>
 * Typically used in repositories or quest structures to signal
 * that an entity with the same ID already exists and cannot be added again.
 * </p>
 *
 * <p>This is an unchecked exception (subclass of {@link RuntimeException}).</p>
 */
public class DuplicateIdException extends RuntimeException {

    /**
     * Creates a new {@code DuplicateIdException} with the given message.
     *
     * @param message description of the error
     */
    public DuplicateIdException(String message) {
        super(message);
    }
}
