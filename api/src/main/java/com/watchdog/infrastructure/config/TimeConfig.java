package com.watchdog.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Application-wide beans. A single {@link Clock} makes time injectable and testable
 * (no hidden Instant.now() calls in components) — the catch-time stat depends on an
 * honest, controllable clock.
 */
@Configuration
public class TimeConfig {

    @Bean
    Clock systemClock() {
        return Clock.systemUTC();
    }
}
