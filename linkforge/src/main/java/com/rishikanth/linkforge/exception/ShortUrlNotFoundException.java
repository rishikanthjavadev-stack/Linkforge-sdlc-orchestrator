package com.rishikanth.linkforge.exception;

public class ShortUrlNotFoundException extends RuntimeException {
    public ShortUrlNotFoundException(String code) {
        super("No short URL found for code: " + code);
    }
}
