package com.watchdog.infrastructure.ats.greenhouse;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import org.springframework.web.util.HtmlUtils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a Greenhouse board JSON payload
 * ({@code /v1/boards/{slug}/jobs?content=true}) into normalized {@link Posting}s.
 * Pure: JSON string + context in, domain objects out, no I/O.
 *
 * <p>Field mapping (measured against a real Stripe board):
 * <ul>
 *   <li>{@code id} → atsPostingId</li>
 *   <li>{@code title}, {@code location.name}, {@code absolute_url}</li>
 *   <li>{@code content} → description, with HTML entities decoded so keyword
 *       matching (S6) reads real text</li>
 *   <li>{@code first_published} → postedAt (the real posting time — drives the
 *       catch-time signature stat); falls back to {@code updated_at}</li>
 *   <li>{@code departments[0].name} → department</li>
 * </ul>
 * Greenhouse exposes no structured salary or employment-type; those stay
 * empty/UNKNOWN here and are best-effort parsed from the body in S6.
 */
public class GreenhouseParser {

    private final ObjectMapper mapper;

    public GreenhouseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * @param body       raw JSON from the board endpoint
     * @param companyId  the company these postings belong to
     * @param firstSeenAt observation time (our clock) stamped on every posting
     */
    public List<Posting> parse(String body, CompanyId companyId, Instant firstSeenAt) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw new GreenhouseParseException("Failed to parse Greenhouse JSON", e);
        }

        JsonNode jobs = root.path("jobs");
        List<Posting> result = new ArrayList<>();
        for (JsonNode job : jobs) {
            result.add(toPosting(job, companyId, firstSeenAt));
        }
        return result;
    }

    private Posting toPosting(JsonNode job, CompanyId companyId, Instant firstSeenAt) {
        String atsPostingId = job.path("id").asText();
        String title = text(job, "title");
        String location = job.path("location").path("name").isMissingNode()
                ? null : emptyToNull(job.path("location").path("name").asText());
        String url = text(job, "absolute_url");
        String description = decodeHtml(text(job, "content"));
        String department = firstDepartment(job);
        Instant postedAt = parseTimestamp(
                firstNonBlank(text(job, "first_published"), text(job, "updated_at")));

        return new Posting(
                PostingId.generate(), companyId, atsPostingId, title, location,
                RemoteType.UNKNOWN, department, EmploymentType.UNKNOWN, Salary.empty(),
                url, description, SponsorshipSignal.UNKNOWN, postedAt, firstSeenAt, job.toString());
    }

    private static String firstDepartment(JsonNode job) {
        JsonNode depts = job.path("departments");
        if (depts.isArray() && !depts.isEmpty()) {
            return emptyToNull(depts.get(0).path("name").asText());
        }
        return null;
    }

    private static Instant parseTimestamp(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception e) {
            return null; // honest: unparseable timestamp -> no postedAt, never faked
        }
    }

    private static String decodeHtml(String s) {
        return s == null ? null : HtmlUtils.htmlUnescape(s);
    }

    private static String text(JsonNode node, String field) {
        JsonNode v = node.path(field);
        return v.isMissingNode() || v.isNull() ? null : v.asText();
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static String firstNonBlank(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }
}
