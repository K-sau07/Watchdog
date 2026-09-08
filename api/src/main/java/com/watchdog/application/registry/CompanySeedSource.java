package com.watchdog.application.registry;

import com.watchdog.domain.model.AtsSource;

import java.util.List;

/**
 * Supplies the seed set of companies to register (D-WD1). Implemented in
 * infrastructure (reads the classpath seed file); abstracted here so
 * {@link DiscoveryService} is unit-testable with a fake source.
 */
public interface CompanySeedSource {

    List<SeedEntry> load();

    /** One verified ATS board: display name, source, and the ATS slug. */
    record SeedEntry(String name, AtsSource source, String slug) {}
}
