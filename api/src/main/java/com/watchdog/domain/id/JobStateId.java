package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a JobState. Wraps a UUID for type-safe references. */
public record JobStateId(UUID value) {

    public JobStateId {
        Objects.requireNonNull(value, "JobStateId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static JobStateId generate() {
        return new JobStateId(UUID.randomUUID());
    }

    public static JobStateId of(UUID value) {
        return new JobStateId(value);
    }

    public static JobStateId of(String value) {
        return new JobStateId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
