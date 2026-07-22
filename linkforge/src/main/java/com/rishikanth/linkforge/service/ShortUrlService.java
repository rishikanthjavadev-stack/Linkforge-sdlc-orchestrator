package com.rishikanth.linkforge.service;

import com.rishikanth.linkforge.dto.AnalyticsResponse;
import com.rishikanth.linkforge.dto.ShortenRequest;
import com.rishikanth.linkforge.dto.ShortenResponse;
import com.rishikanth.linkforge.exception.ShortUrlExpiredException;
import com.rishikanth.linkforge.exception.ShortUrlNotFoundException;
import com.rishikanth.linkforge.model.ShortUrl;
import com.rishikanth.linkforge.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ShortUrlService {

    private final ShortUrlRepository repository;
    private final Base62Encoder encoder;
    private final String baseUrl;
    private final int defaultExpiryDays;

    public ShortUrlService(ShortUrlRepository repository,
                            Base62Encoder encoder,
                            @Value("${app.shortener.base-url}") String baseUrl,
                            @Value("${app.shortener.default-expiry-days}") int defaultExpiryDays) {
        this.repository = repository;
        this.encoder = encoder;
        this.baseUrl = baseUrl;
        this.defaultExpiryDays = defaultExpiryDays;
    }

    /**
     * Creates a new short URL.
     *
     * Sequencing note (relevant for Phase 2 orchestration mapping):
     * the entity must be persisted once to obtain its DB-generated id
     * BEFORE the Base62 code can be derived, then persisted again to
     * store that code. This two-phase write is a known trade-off of the
     * counter-based encoding strategy - documented in architecture-decisions.md.
     */
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        int expiryDays = request.getExpiryDays() != null ? request.getExpiryDays() : defaultExpiryDays;
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(expiryDays);

        ShortUrl entity = new ShortUrl(request.getLongUrl(), expiresAt);
        entity = repository.save(entity); // phase 1: obtain id

        String code = encoder.encode(entity.getId());
        entity.assignCode(code);
        entity = repository.save(entity); // phase 2: persist code

        String shortUrl = baseUrl + "/" + code;
        return new ShortenResponse(code, shortUrl, entity.getLongUrl(),
                entity.getCreatedAt(), entity.getExpiresAt());
    }

    /**
     * Resolves a code to its target URL and records a click.
     * Used by the redirect endpoint (GET /{code}).
     */
    @Transactional
    public String resolveAndRecordClick(String code) {
        ShortUrl entity = repository.findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));

        if (!entity.isActive() || entity.isExpired()) {
            throw new ShortUrlExpiredException(code);
        }

        entity.recordClick();
        repository.save(entity);
        return entity.getLongUrl();
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse getAnalytics(String code) {
        ShortUrl entity = repository.findByCode(code)
                .orElseThrow(() -> new ShortUrlNotFoundException(code));

        return new AnalyticsResponse(
                entity.getCode(),
                entity.getLongUrl(),
                entity.getClickCount(),
                entity.getCreatedAt(),
                entity.getLastAccessedAt(),
                entity.isActive(),
                entity.isExpired()
        );
    }
}
