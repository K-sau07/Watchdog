package com.watchdog.infrastructure.registry;

import com.watchdog.application.registry.CompanySeedSource;
import com.watchdog.domain.model.AtsSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the verified company seed set from a classpath JSON resource
 * (default {@code classpath:seed/companies-seed.json}). Skips malformed entries
 * defensively so one bad row can't break startup seeding.
 */
@Component
public class JsonCompanySeedSource implements CompanySeedSource {

    private final Resource seedResource;
    private final ObjectMapper mapper;

    public JsonCompanySeedSource(
            @Value("${watchdog.registry.seed-file:classpath:seed/companies-seed.json}") Resource seedResource,
            ObjectMapper mapper) {
        this.seedResource = seedResource;
        this.mapper = mapper;
    }

    @Override
    public List<SeedEntry> load() {
        JsonNode root;
        try (InputStream in = seedResource.getInputStream()) {
            root = mapper.readTree(in);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to read company seed file: " + seedResource, e);
        }

        List<SeedEntry> entries = new ArrayList<>();
        for (JsonNode node : root.path("companies")) {
            SeedEntry entry = toEntry(node);
            if (entry != null) {
                entries.add(entry);
            }
        }
        return entries;
    }

    private static SeedEntry toEntry(JsonNode node) {
        String name = text(node, "name");
        String sourceRaw = text(node, "source");
        String slug = text(node, "slug");
        if (name == null || sourceRaw == null || slug == null) {
            return null; // skip incomplete rows
        }
        AtsSource source;
        try {
            source = AtsSource.valueOf(sourceRaw);
        } catch (IllegalArgumentException e) {
            return null; // skip unknown source values
        }
        return new SeedEntry(name, source, slug);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() || v.asText().isBlank() ? null : v.asText();
    }
}
