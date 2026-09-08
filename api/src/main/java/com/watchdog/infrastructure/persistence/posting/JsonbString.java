package com.watchdog.infrastructure.persistence.posting;

/**
 * A thin wrapper marking a String as jsonb content. Using a dedicated type (rather
 * than raw String) lets the JDBC converters target ONLY jsonb columns — converting
 * String&lt;-&gt;jsonb globally would wrongly coerce every String column. Null-safe:
 * a null payload is represented by a JsonbString with a null value.
 */
public record JsonbString(String value) {

    public static JsonbString of(String value) {
        return new JsonbString(value);
    }
}
