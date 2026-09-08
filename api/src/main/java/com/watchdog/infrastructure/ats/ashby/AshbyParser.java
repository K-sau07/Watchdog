package com.watchdog.infrastructure.ats.ashby;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses an Ashby job-board JSON payload ({@code /posting-api/job-board/{slug}}) into
 * normalized {@link Posting}s. Pure: JSON string + context in, domain objects out.
 *
 * <p>Field mapping (measured against the real linear board). Ashby is the richest of
 * the three sources:
 * <ul>
 *   <li>{@code jobs[]} wrapped under a top-level object (with {@code apiVersion})</li>
 *   <li>{@code id}, {@code title}, {@code location}, {@code department}</li>
 *   <li>{@code employmentType} (FullTime/Intern/Contract/PartTime) → EmploymentType —
 *       a REAL structured type (neither Greenhouse nor Lever expose this)</li>
 *   <li>{@code workplaceType} (Remote/Hybrid/Onsite) with {@code isRemote} fallback
 *       → RemoteType</li>
 *   <li>{@code publishedAt} (ISO) → postedAt (drives catch-time)</li>
 *   <li>{@code jobUrl} → url; {@code descriptionPlain} → description</li>
 * </ul>
 * Salary occasionally present ({@code compensation}) but structure varies; left empty
 * here, best-effort in S6.
 */
public class AshbyParser {

    private final ObjectMapper mapper;

    public AshbyParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<Posting> parse(String body, CompanyId companyId, Instant firstSeenAt) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw new AshbyParseException("Failed to parse Ashby JSON", e);
        }
        JsonNode jobs = root.path("jobs");
        if (!jobs.isArray()) {
            throw new AshbyParseException("Expected a 'jobs' array", null);
        }
        List<Posting> result = new ArrayList<>();
        for (JsonNode job : jobs) {
            result.add(toPosting(job, companyId, firstSeenAt));
        }
        return result;
    }

    private Posting toPosting(JsonNode job, CompanyId companyId, Instant firstSeenAt) {
        String atsPostingId = text(job, "id");
        String title = text(job, "title");
        String location = emptyToNull(text(job, "location"));
        String department = firstNonBlank(text(job, "department"), text(job, "team"));
        EmploymentType employmentType = mapEmploymentType(text(job, "employmentType"));
        RemoteType remoteType = mapRemote(text(job, "workplaceType"), job.path("isRemote"));
        String url = text(job, "jobUrl");
        String description = firstNonBlank(text(job, "descriptionPlain"), text(job, "descriptionHtml"));
        Instant postedAt = parseIso(text(job, "publishedAt"));

        return new Posting(
                PostingId.generate(), companyId, atsPostingId, title, location,
                remoteType, department, employmentType, Salary.empty(),
                url, description, SponsorshipSignal.UNKNOWN, postedAt, firstSeenAt, job.toString());
    }

    private static EmploymentType mapEmploymentType(String et) {
        if (et == null) return EmploymentType.UNKNOWN;
        return switch (et.toLowerCase(Locale.ROOT)) {
            case "fulltime", "full_time" -> EmploymentType.FULL_TIME;
            case "intern", "internship" -> EmploymentType.INTERNSHIP;
            case "contract", "contractor", "temporary" -> EmploymentType.CONTRACT;
            case "parttime", "part_time" -> EmploymentType.PART_TIME;
            default -> EmploymentType.UNKNOWN;
        };
    }

    private static RemoteType mapRemote(String workplaceType, JsonNode isRemote) {
        if (workplaceType != null) {
            switch (workplaceType.toLowerCase(Locale.ROOT)) {
                case "remote" -> { return RemoteType.REMOTE; }
                case "hybrid" -> { return RemoteType.HYBRID; }
                case "onsite", "on-site" -> { return RemoteType.ONSITE; }
                default -> { /* fall through to isRemote */ }
            }
        }
        if (isRemote != null && isRemote.isBoolean()) {
            return isRemote.asBoolean() ? RemoteType.REMOTE : RemoteType.ONSITE;
        }
        return RemoteType.UNKNOWN;
    }

    private static Instant parseIso(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return OffsetDateTime.parse(raw).toInstant();
        } catch (Exception e) {
            return null; // honest: unparseable -> no postedAt
        }
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
