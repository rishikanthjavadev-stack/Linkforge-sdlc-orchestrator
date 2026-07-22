package com.rishikanth.linkforge.dto;

import java.time.LocalDateTime;

public class AnalyticsResponse {
    private String code;
    private String longUrl;
    private long clickCount;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessedAt;
    private boolean active;
    private boolean expired;

    public AnalyticsResponse(String code, String longUrl, long clickCount,
                              LocalDateTime createdAt, LocalDateTime lastAccessedAt,
                              boolean active, boolean expired) {
        this.code = code;
        this.longUrl = longUrl;
        this.clickCount = clickCount;
        this.createdAt = createdAt;
        this.lastAccessedAt = lastAccessedAt;
        this.active = active;
        this.expired = expired;
    }

    public String getCode() { return code; }
    public String getLongUrl() { return longUrl; }
    public long getClickCount() { return clickCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public boolean isActive() { return active; }
    public boolean isExpired() { return expired; }
}
