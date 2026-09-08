package com.watchdog.infrastructure.ats.greenhouse;

import tools.jackson.databind.ObjectMapper;
import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.Posting;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GreenhouseParserTest {

    private final GreenhouseParser parser = new GreenhouseParser(new ObjectMapper());
    private final CompanyId companyId = CompanyId.generate();
    private final Instant seenAt = Instant.parse("2026-09-08T12:00:00Z");

    private String fixture() throws IOException {
        try (var in = getClass().getResourceAsStream("/fixtures/greenhouse/board.json")) {
            assertThat(in).as("fixture present").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesAllJobsFromFixture() throws IOException {
        List<Posting> postings = parser.parse(fixture(), companyId, seenAt);
        assertThat(postings).hasSize(3);
    }

    @Test
    void mapsCoreFields() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.atsPostingId()).isEqualTo("8172487");
        assertThat(p.title()).isEqualTo("Abuse Investigator");
        assertThat(p.location()).isEqualTo("Dublin");
        assertThat(p.url()).contains("gh_jid=8172487");
        assertThat(p.companyId()).isEqualTo(companyId);
        assertThat(p.firstSeenAt()).isEqualTo(seenAt);
    }

    @Test
    void postedAtComesFromFirstPublished() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        // fixture first_published = 2026-09-03T13:30:34-04:00
        assertThat(p.postedAtOpt()).contains(Instant.parse("2026-09-03T17:30:34Z"));
    }

    @Test
    void descriptionHtmlEntitiesDecoded() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        // raw fixture content has &lt;h2&gt; — decoded it becomes real angle brackets
        assertThat(p.description()).contains("<h2>").doesNotContain("&lt;h2&gt;");
    }

    @Test
    void rawJsonPreserved() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.rawJson()).contains("\"id\":8172487");
    }

    @Test
    void unstructuredFieldsAreUnknown() throws IOException {
        // Greenhouse gives no structured salary/employment-type -> empty/UNKNOWN in S3.
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.salary().isEmpty()).isTrue();
        assertThat(p.employmentType().name()).isEqualTo("UNKNOWN");
    }

    @Test
    void malformedJsonThrows() {
        assertThatThrownBy(() -> parser.parse("{ not json", companyId, seenAt))
                .isInstanceOf(GreenhouseParseException.class);
    }

    @Test
    void emptyBoardYieldsNoPostings() {
        assertThat(parser.parse("{\"jobs\":[]}", companyId, seenAt)).isEmpty();
    }
}
