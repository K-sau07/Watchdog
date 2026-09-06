package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a User. Wraps a UUID for type-safe references. */
public record UserId(UUID value) {

    public UserId {
        Objects.requireNonNull(value, "UserId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static UserId generate() {
        return new UserId(UUID.randomUUID());
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId of(String value) {
        return new UserId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
