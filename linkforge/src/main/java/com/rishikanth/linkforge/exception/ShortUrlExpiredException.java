package com.rishikanth.linkforge.exception;

public class ShortUrlExpiredException extends RuntimeException {
    public ShortUrlExpiredException(String code) {
        super("Short URL has expired or is inactive: " + code);
    }
}
