package com.watchdog.domain.id;

import java.util.Objects;
import java.util.UUID;

/** Typed identifier for an OutreachEvent. */
public record OutreachEventId(UUID value) {

    public OutreachEventId {
        Objects.requireNonNull(value, "OutreachEventId value must not be null");
    }

    public static OutreachEventId generate() {
        return new OutreachEventId(UUID.randomUUID());
    }

    public static OutreachEventId of(UUID value) {
        return new OutreachEventId(value);
    }

    public static OutreachEventId of(String value) {
        return new OutreachEventId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
