package com.example.escort.common;

import lombok.Getter;

@Getter
public class AiApiException extends RuntimeException {
    private final int statusCode;
    public AiApiException(int statusCode, String message) {
        super(message); this.statusCode = statusCode;
    }
}