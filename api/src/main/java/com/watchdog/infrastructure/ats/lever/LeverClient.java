package com.watchdog.infrastructure.ats.lever;

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
 * {@link JobSourcePort} for Lever. Fetches a company's board JSON over HTTP and
 * delegates normalization to {@link LeverParser}. Base URL configurable (prod
 * default = the public API; tests point at a mock server).
 */
@Component
public class LeverClient implements JobSourcePort {

    private final RestClient restClient;
    private final LeverParser parser;
    private final Clock clock;

    public LeverClient(
            @Value("${watchdog.ats.lever.base-url:https://api.lever.co}") String baseUrl,
            ObjectMapper objectMapper,
            Clock clock) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.parser = new LeverParser(objectMapper);
        this.clock = clock;
    }

    @Override
    public AtsSource source() {
        return AtsSource.LEVER;
    }

    @Override
    public List<Posting> fetchPostings(Company company) {
        String body = restClient.get()
                .uri("/v0/postings/{slug}?mode=json", company.atsSlug())
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return List.of();
        }
        return parser.parse(body, company.id(), Instant.now(clock));
    }
}
