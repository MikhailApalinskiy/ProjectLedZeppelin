package com.javarush.apalinskiy.exceptions;

public class DuplicateIdException extends RuntimeException {
    public DuplicateIdException(String message) {
        super(message);
    }

    public DuplicateIdException() {
    }
}
