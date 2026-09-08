package com.watchdog.registry;

import com.watchdog.application.registry.CompanySeedSource.SeedEntry;
import com.watchdog.infrastructure.registry.JsonCompanySeedSource;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the real seed data file: it parses, every row is complete + valid, and there
 * are no duplicate (source, slug) natural keys (a dup would be a silent no-op waste).
 */
class JsonCompanySeedSourceTest {

    private final JsonCompanySeedSource source = new JsonCompanySeedSource(
            new ClassPathResource("seed/companies-seed.json"), new ObjectMapper());

    @Test
    void loadsTheRealSeedFile() {
        List<SeedEntry> entries = source.load();
        assertThat(entries).isNotEmpty();
        // every entry is fully populated
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.name()).isNotBlank();
            assertThat(e.slug()).isNotBlank();
            assertThat(e.source()).isNotNull();
        });
    }

    @Test
    void noDuplicateNaturalKeys() {
        List<SeedEntry> entries = source.load();
        List<String> keys = entries.stream()
                .map(e -> e.source() + ":" + e.slug())
                .toList();
        List<String> distinct = keys.stream().distinct().collect(Collectors.toList());
        assertThat(keys).as("no duplicate (source, slug) rows").hasSameSizeAs(distinct);
    }

    @Test
    void coversAllThreeAtsSources() {
        List<SeedEntry> entries = source.load();
        assertThat(entries.stream().map(SeedEntry::source).distinct())
                .as("seed spans Greenhouse, Lever, and Ashby")
                .hasSize(3);
    }
}
