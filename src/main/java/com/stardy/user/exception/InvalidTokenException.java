package com.stardy.user.exception;

public class InvalidTokenException extends  RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
