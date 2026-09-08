package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Posting. Wraps a UUID for type-safe references. */
public record PostingId(UUID value) {

    public PostingId {
        Objects.requireNonNull(value, "PostingId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static PostingId generate() {
        return new PostingId(UUID.randomUUID());
    }

    public static PostingId of(UUID value) {
        return new PostingId(value);
    }

    public static PostingId of(String value) {
        return new PostingId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
