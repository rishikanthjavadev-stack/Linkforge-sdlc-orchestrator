package com.rishikanth.linkforge.dto;

import java.time.LocalDateTime;

public class ShortenResponse {
    private String code;
    private String shortUrl;
    private String longUrl;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    public ShortenResponse(String code, String shortUrl, String longUrl,
                            LocalDateTime createdAt, LocalDateTime expiresAt) {
        this.code = code;
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getCode() { return code; }
    public String getShortUrl() { return shortUrl; }
    public String getLongUrl() { return longUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
