package com.watchdog.infrastructure.ats.ashby;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AshbyParserTest {

    private final AshbyParser parser = new AshbyParser(new ObjectMapper());
    private final CompanyId companyId = CompanyId.generate();
    private final Instant seenAt = Instant.parse("2026-09-08T12:00:00Z");

    private String fixture() throws IOException {
        try (var in = getClass().getResourceAsStream("/fixtures/ashby/board.json")) {
            assertThat(in).as("fixture present").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void parsesAllJobs() throws IOException {
        assertThat(parser.parse(fixture(), companyId, seenAt)).hasSize(3);
    }

    @Test
    void mapsCoreFields() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.atsPostingId()).isEqualTo("d3bc1ced-3ce4-4086-a050-555055dbb1ff");
        assertThat(p.title()).isEqualTo("Senior / Staff Fullstack Engineer");
        assertThat(p.location()).isEqualTo("Europe");
        assertThat(p.department()).isEqualTo("Product");
        assertThat(p.url()).contains("jobs.ashbyhq.com/linear");
    }

    @Test
    void structuredEmploymentType() throws IOException {
        // Ashby uniquely gives a real employment-type field.
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.employmentType()).isEqualTo(EmploymentType.FULL_TIME);
    }

    @Test
    void remoteTypeFromWorkplaceType() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.remoteType()).isEqualTo(RemoteType.REMOTE);
    }

    @Test
    void publishedAtBecomesPostedAt() throws IOException {
        Posting p = parser.parse(fixture(), companyId, seenAt).getFirst();
        assertThat(p.postedAtOpt()).contains(Instant.parse("2021-04-27T20:13:45.158Z"));
    }

    @Test
    void employmentTypeMappingCoversAllValues() {
        String body = """
            {"jobs":[
              {"id":"1","title":"A","employmentType":"Intern","publishedAt":null},
              {"id":"2","title":"B","employmentType":"Contract","publishedAt":null},
              {"id":"3","title":"C","employmentType":"PartTime","publishedAt":null},
              {"id":"4","title":"D","employmentType":"Weird","publishedAt":null}
            ]}""";
        var postings = parser.parse(body, companyId, seenAt);
        assertThat(postings.get(0).employmentType()).isEqualTo(EmploymentType.INTERNSHIP);
        assertThat(postings.get(1).employmentType()).isEqualTo(EmploymentType.CONTRACT);
        assertThat(postings.get(2).employmentType()).isEqualTo(EmploymentType.PART_TIME);
        assertThat(postings.get(3).employmentType()).isEqualTo(EmploymentType.UNKNOWN);
    }

    @Test
    void isRemoteFallbackWhenNoWorkplaceType() {
        String body = """
            {"jobs":[{"id":"1","title":"A","isRemote":false,"publishedAt":null}]}""";
        var p = parser.parse(body, companyId, seenAt).getFirst();
        assertThat(p.remoteType()).isEqualTo(RemoteType.ONSITE);
    }

    @Test
    void missingJobsArrayThrows() {
        assertThatThrownBy(() -> parser.parse("{\"apiVersion\":\"1\"}", companyId, seenAt))
                .isInstanceOf(AshbyParseException.class);
    }
}
