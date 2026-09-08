package com.watchdog.infrastructure.ats.greenhouse;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.port.JobSourcePort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

/**
 * {@link JobSourcePort} for Greenhouse. Fetches a company's board JSON over HTTP and
 * delegates normalization to {@link GreenhouseParser}. The base URL is configurable
 * (prod default = the public API; tests point it at a mock server).
 *
 * <p>{@code firstSeenAt} is stamped from the injected {@link Clock} at fetch time —
 * this is our observation clock and feeds the catch-time stat.
 */
@Component
public class GreenhouseClient implements JobSourcePort {

    private final RestClient restClient;
    private final GreenhouseParser parser;
    private final Clock clock;

    public GreenhouseClient(
            @Value("${watchdog.ats.greenhouse.base-url:https://boards-api.greenhouse.io}") String baseUrl,
            ObjectMapper objectMapper,
            Clock clock) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.parser = new GreenhouseParser(objectMapper);
        this.clock = clock;
    }

    @Override
    public AtsSource source() {
        return AtsSource.GREENHOUSE;
    }

    @Override
    public List<Posting> fetchPostings(Company company) {
        String body = restClient.get()
                .uri("/v1/boards/{slug}/jobs?content=true", company.atsSlug())
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return List.of();
        }
        Instant firstSeenAt = Instant.now(clock);
        return parser.parse(body, company.id(), firstSeenAt);
    }
}
