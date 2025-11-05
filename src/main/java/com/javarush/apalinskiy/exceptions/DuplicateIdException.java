package com.javarush.apalinskiy.exceptions;

/**
 * Exception thrown when an attempt is made to create or register
 * an entity with a duplicate identifier.
 *
 * <p>This exception is typically used to indicate logical conflicts
 * in domain-level operations such as quest creation, user registration,
 * or database imports where unique IDs must be enforced.</p>
 */
public class DuplicateIdException extends RuntimeException {

    /**
     * Constructs a new {@code DuplicateIdException} with the specified detail message.
     *
     * @param message description of the duplication conflict
     */
    public DuplicateIdException(String message) {
        super(message);
    }
}
