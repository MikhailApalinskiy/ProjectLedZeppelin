package com.javarush.apalinskiy.exceptions;

public class DuplicateLoginException extends RuntimeException {
    public DuplicateLoginException(String message) {
        super(message);
    }

    public DuplicateLoginException() {
    }
}
