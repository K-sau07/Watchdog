package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a FilterProfile. Wraps a UUID for type-safe references. */
public record FilterProfileId(UUID value) {

    public FilterProfileId {
        Objects.requireNonNull(value, "FilterProfileId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static FilterProfileId generate() {
        return new FilterProfileId(UUID.randomUUID());
    }

    public static FilterProfileId of(UUID value) {
        return new FilterProfileId(value);
    }

    public static FilterProfileId of(String value) {
        return new FilterProfileId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
