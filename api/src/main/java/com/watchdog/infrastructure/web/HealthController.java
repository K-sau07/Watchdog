package com.watchdog.infrastructure.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Liveness endpoint for the dashboard and uptime checks.
 * Deliberately trivial in S0 — a stable contract the frontend can hit
 * before any domain logic exists.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    /** Minimal typed health payload. */
    public record HealthStatus(String status) {}

    @GetMapping("/health")
    public HealthStatus health() {
        return new HealthStatus("UP");
    }
}
