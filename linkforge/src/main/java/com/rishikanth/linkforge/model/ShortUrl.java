package com.rishikanth.linkforge.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Core schema for a shortened URL.
 *
 * Design notes (see docs/architecture-decisions.md for full rationale):
 * - id is an auto-increment surrogate key; the short "code" is derived
 *   from this id via Base62 encoding at write time, not stored redundantly
 *   as the primary key, so lookups by code still hit an indexed unique column.
 * - clickCount is denormalized onto this row for O(1) read on the hot path
 *   (GET /api/analytics/{code}). A separate ClickEvent table is deliberately
 *   NOT included in Phase 1 scope - see assumptions/limitations doc.
 */
@Entity
@Table(name = "short_urls", indexes = {
        @Index(name = "idx_short_code", columnList = "code", unique = true)
})
public class ShortUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Nullable at the DB level: the two-phase write (see ShortUrlService.shorten)
    // persists this entity once to obtain a DB-generated id BEFORE the Base62
    // code can be derived from it, so code is transiently null on the first
    // insert. Uniqueness is still enforced via the index below; application
    // code guarantees code is always populated by the time a row is readable
    // via findByCode().
    @Column(nullable = true, unique = true, length = 16)
    private String code;

    @Column(nullable = false, length = 2048)
    private String longUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime expiresAt;

    private LocalDateTime lastAccessedAt;

    @Column(nullable = false)
    private long clickCount = 0L;

    @Column(nullable = false)
    private boolean active = true;

    protected ShortUrl() {
        // JPA
    }

    public ShortUrl(String longUrl, LocalDateTime expiresAt) {
        this.longUrl = longUrl;
        this.createdAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.active = true;
    }

    public void assignCode(String code) {
        this.code = code;
    }

    public void recordClick() {
        this.clickCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // --- getters ---
    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getLongUrl() { return longUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public long getClickCount() { return clickCount; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
