package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Company. Wraps a UUID for type-safe references. */
public record CompanyId(UUID value) {

    public CompanyId {
        Objects.requireNonNull(value, "CompanyId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static CompanyId generate() {
        return new CompanyId(UUID.randomUUID());
    }

    public static CompanyId of(UUID value) {
        return new CompanyId(value);
    }

    public static CompanyId of(String value) {
        return new CompanyId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
