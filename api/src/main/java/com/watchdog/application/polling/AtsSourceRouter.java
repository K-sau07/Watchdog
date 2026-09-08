package com.watchdog.application.polling;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.port.JobSourcePort;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Routes a company's {@link AtsSource} to the {@link JobSourcePort} that serves it.
 * Spring injects all available port beans; the router indexes them by their declared
 * {@link JobSourcePort#source()}. Adding a new ATS adapter wires itself in automatically.
 */
@Component
public class AtsSourceRouter {

    private final Map<AtsSource, JobSourcePort> bySource = new EnumMap<>(AtsSource.class);

    public AtsSourceRouter(List<JobSourcePort> ports) {
        for (JobSourcePort port : ports) {
            JobSourcePort existing = bySource.putIfAbsent(port.source(), port);
            if (existing != null) {
                throw new IllegalStateException(
                        "Two JobSourcePorts claim source " + port.source() + ": "
                                + existing.getClass().getSimpleName() + " and "
                                + port.getClass().getSimpleName());
            }
        }
    }

    /**
     * @return the port for this source
     * @throws IllegalStateException if no adapter serves it (misconfiguration)
     */
    public JobSourcePort routeTo(AtsSource source) {
        JobSourcePort port = bySource.get(source);
        if (port == null) {
            throw new IllegalStateException("No JobSourcePort registered for source " + source);
        }
        return port;
    }

    /** Sources that currently have an adapter (for diagnostics). */
    public java.util.Set<AtsSource> supportedSources() {
        return java.util.EnumSet.copyOf(bySource.keySet());
    }
}
