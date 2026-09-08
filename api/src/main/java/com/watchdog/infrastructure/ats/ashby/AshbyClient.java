package com.watchdog.infrastructure.ats.ashby;

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
 * {@link JobSourcePort} for Ashby. Fetches a company's job board over HTTP and
 * delegates normalization to {@link AshbyParser}. Base URL configurable (prod
 * default = the public API; tests point at a mock server).
 */
@Component
public class AshbyClient implements JobSourcePort {

    private final RestClient restClient;
    private final AshbyParser parser;
    private final Clock clock;

    public AshbyClient(
            @Value("${watchdog.ats.ashby.base-url:https://api.ashbyhq.com}") String baseUrl,
            ObjectMapper objectMapper,
            Clock clock) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.parser = new AshbyParser(objectMapper);
        this.clock = clock;
    }

    @Override
    public AtsSource source() {
        return AtsSource.ASHBY;
    }

    @Override
    public List<Posting> fetchPostings(Company company) {
        String body = restClient.get()
                .uri("/posting-api/job-board/{slug}", company.atsSlug())
                .retrieve()
                .body(String.class);

        if (body == null || body.isBlank()) {
            return List.of();
        }
        return parser.parse(body, company.id(), Instant.now(clock));
    }
}
