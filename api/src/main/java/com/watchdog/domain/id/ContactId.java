package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for a Contact. Wraps a UUID for type-safe references. */
public record ContactId(UUID value) {

    public ContactId {
        Objects.requireNonNull(value, "ContactId value must not be null");
    }

    /** Mint a fresh identifier (domain-generated, no DB round-trip). */
    public static ContactId generate() {
        return new ContactId(UUID.randomUUID());
    }

    public static ContactId of(UUID value) {
        return new ContactId(value);
    }

    public static ContactId of(String value) {
        return new ContactId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
