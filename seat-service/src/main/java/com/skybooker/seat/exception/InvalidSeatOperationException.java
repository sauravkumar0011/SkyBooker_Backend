package com.skybooker.seat.exception;

public class InvalidSeatOperationException extends RuntimeException {
    public InvalidSeatOperationException(String message) {
        super(message);
    }
}