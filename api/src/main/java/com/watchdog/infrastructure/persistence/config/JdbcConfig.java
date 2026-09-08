package com.watchdog.infrastructure.persistence.config;

import com.watchdog.infrastructure.persistence.posting.JsonbString;
import org.postgresql.util.PGobject;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.jdbc.repository.config.AbstractJdbcConfiguration;

import java.sql.SQLException;
import java.util.List;

/**
 * Spring Data JDBC configuration. Registers custom converters so a {@link JsonbString}
 * maps to/from a Postgres {@code jsonb} column via the driver's {@link PGobject}.
 * The dedicated wrapper type keeps the conversion scoped to jsonb columns only.
 */
@Configuration
public class JdbcConfig extends AbstractJdbcConfiguration {

    @Override
    protected List<?> userConverters() {
        return List.of(new JsonbWritingConverter(), new JsonbReadingConverter());
    }

    @WritingConverter
    static class JsonbWritingConverter implements org.springframework.core.convert.converter.Converter<JsonbString, PGobject> {
        @Override
        public PGobject convert(JsonbString source) {
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            try {
                pg.setValue(source.value());
            } catch (SQLException e) {
                throw new IllegalStateException("Failed to set jsonb value", e);
            }
            return pg;
        }
    }

    @ReadingConverter
    static class JsonbReadingConverter implements org.springframework.core.convert.converter.Converter<PGobject, JsonbString> {
        @Override
        public JsonbString convert(PGobject source) {
            return new JsonbString(source.getValue());
        }
    }
}
