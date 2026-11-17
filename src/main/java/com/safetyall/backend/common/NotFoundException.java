package com.safetyall.backend.common;

/** 대상이 없다(HTTP 404). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
