package com.skybooker.booking.exception;

public class DuplicatePnrException extends RuntimeException {
    public DuplicatePnrException(String message) {
        super(message);
    }
}