package com.watchdog.infrastructure.ats.lever;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LeverParserTest {

    private final LeverParser parser = new LeverParser(new ObjectMapper());
    private final CompanyId companyId = CompanyId.generate();
    private final Instant seenAt = Instant.parse("2026-09-08T12:00:00Z");

    private String fixture() throws IOException {
        try (var in = getClass().getResourceAsStream("/fixtures/lever/board.json")) {
            assertThat(in).as("fixture present").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesAllJobs() throws IOException {
        assertThat(parser.parse(fixture(), companyId, seenAt)).hasSize(3);
    }

    @Test
    void mapsLeverSpecificFields() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.atsPostingId()).isEqualTo("681fbc53-1e34-4a46-8677-3a78118674eb");
        assertThat(p.title()).isEqualTo("Approved Professional 3"); // Lever's "text"
        assertThat(p.location()).isEqualTo("Baltimore, MD");        // categories.location
        assertThat(p.department()).isEqualTo("Operations");         // categories.team
        assertThat(p.url()).contains("jobs.lever.co/leverdemo");
        assertThat(p.firstSeenAt()).isEqualTo(seenAt);
    }

    @Test
    void workplaceTypeMapsToRemoteType() throws IOException {
        List<Posting> postings = parser.parse(fixture(), companyId, seenAt);
        assertThat(postings.get(0).remoteType()).isEqualTo(RemoteType.REMOTE);  // "remote"
        assertThat(postings.get(1).remoteType()).isEqualTo(RemoteType.HYBRID);  // "hybrid"
    }

    @Test
    void createdAtEpochMillisBecomesPostedAt() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        // fixture createdAt = 1565990241800 ms
        assertThat(p.postedAtOpt()).contains(Instant.ofEpochMilli(1565990241800L));
    }

    @Test
    void descriptionUsesPlainText() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.description()).contains("Demo Job Listing").doesNotContain("<div>");
    }

    @Test
    void nonArrayThrows() {
        assertThatThrownBy(() -> parser.parse("{\"ok\":false}", companyId, seenAt))
                .isInstanceOf(LeverParseException.class);
    }

    @Test
    void emptyArrayYieldsNoPostings() {
        assertThat(parser.parse("[]", companyId, seenAt)).isEmpty();
    }
}
