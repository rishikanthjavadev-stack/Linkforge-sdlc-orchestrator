package com.rishikanth.linkforge.controller;

import com.rishikanth.linkforge.dto.AnalyticsResponse;
import com.rishikanth.linkforge.dto.ShortenRequest;
import com.rishikanth.linkforge.dto.ShortenResponse;
import com.rishikanth.linkforge.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class LinkController {

    private final ShortUrlService service;

    public LinkController(ShortUrlService service) {
        this.service = service;
    }

    @PostMapping("/shorten")
    public ResponseEntity<ShortenResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortenResponse response = service.shorten(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/analytics/{code}")
    public ResponseEntity<AnalyticsResponse> analytics(@PathVariable String code) {
        return ResponseEntity.ok(service.getAnalytics(code));
    }
}
