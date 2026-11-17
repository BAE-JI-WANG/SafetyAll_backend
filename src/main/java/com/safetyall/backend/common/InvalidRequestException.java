package com.safetyall.backend.common;

/** 요청 값이 잘못되었다(HTTP 400). */
public class InvalidRequestException extends RuntimeException {

    public InvalidRequestException(String message) {
        super(message);
    }
}
