package com.watchdog.infrastructure.ats.lever;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LeverClientTest {

    private MockWebServer server;
    private LeverClient client;
    private final Instant fixedNow = Instant.parse("2026-09-08T12:00:00Z");

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        client = new LeverClient(
                server.url("/").toString().replaceAll("/$", ""),
                new ObjectMapper(),
                Clock.fixed(fixedNow, ZoneOffset.UTC));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    private String fixture() throws IOException {
        try (var in = getClass().getResourceAsStream("/fixtures/lever/board.json")) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void fetchesAndParsesBoard() throws Exception {
        server.enqueue(new MockResponse()
                .setBody(fixture())
                .addHeader("Content-Type", "application/json"));

        Company company = Company.register("LeverDemo", AtsSource.LEVER, "leverdemo");
        List<Posting> postings = client.fetchPostings(company);

        assertThat(postings).hasSize(3);
        assertThat(postings.getFirst().firstSeenAt()).isEqualTo(fixedNow);
        assertThat(postings.getFirst().companyId()).isEqualTo(company.id());

        RecordedRequest request = server.takeRequest();
        assertThat(request.getPath()).isEqualTo("/v0/postings/leverdemo?mode=json");
    }

    @Test
    void sourceIsLever() {
        assertThat(client.source()).isEqualTo(AtsSource.LEVER);
    }

    @Test
    void emptyArrayYieldsNoPostings() throws Exception {
        server.enqueue(new MockResponse().setBody("[]").addHeader("Content-Type", "application/json"));
        Company company = Company.register("Empty", AtsSource.LEVER, "empty");
        assertThat(client.fetchPostings(company)).isEmpty();
    }
}
