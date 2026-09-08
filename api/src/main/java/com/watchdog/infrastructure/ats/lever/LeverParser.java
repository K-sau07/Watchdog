package com.watchdog.infrastructure.ats.lever;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Parses a Lever board JSON payload ({@code /v0/postings/{slug}?mode=json}) into
 * normalized {@link Posting}s. Pure: JSON string + context in, domain objects out.
 *
 * <p>Field mapping (measured against the real leverdemo board):
 * <ul>
 *   <li>{@code id} (UUID string) → atsPostingId</li>
 *   <li>{@code text} → title (Lever's title field is "text", not "title")</li>
 *   <li>{@code categories.location} → location; {@code categories.team} → department</li>
 *   <li>{@code workplaceType} (remote/hybrid/onsite) → RemoteType — Lever gives a
 *       real structured remote signal (unlike Greenhouse)</li>
 *   <li>{@code createdAt} (epoch millis) → postedAt (drives catch-time)</li>
 *   <li>{@code descriptionPlain} → description (already plain text; falls back to
 *       {@code description} HTML if plain is absent)</li>
 *   <li>{@code hostedUrl} → url</li>
 * </ul>
 * Lever exposes no structured salary/employment-type → empty/UNKNOWN (best-effort in S6).
 */
public class LeverParser {

    private final ObjectMapper mapper;

    public LeverParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<Posting> parse(String body, CompanyId companyId, Instant firstSeenAt) {
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (Exception e) {
            throw new LeverParseException("Failed to parse Lever JSON", e);
        }
        if (!root.isArray()) {
            throw new LeverParseException("Expected a JSON array of postings", null);
        }
        List<Posting> result = new ArrayList<>();
        for (JsonNode job : root) {
            result.add(toPosting(job, companyId, firstSeenAt));
        }
        return result;
    }

    private Posting toPosting(JsonNode job, CompanyId companyId, Instant firstSeenAt) {
        String atsPostingId = text(job, "id");
        String title = text(job, "text");
        JsonNode categories = job.path("categories");
        String location = emptyToNull(categories.path("location").asText(null));
        String department = emptyToNull(categories.path("team").asText(null));
        RemoteType remoteType = mapWorkplaceType(text(job, "workplaceType"));
        String url = text(job, "hostedUrl");
        String description = firstNonBlank(text(job, "descriptionPlain"), text(job, "description"));
        Instant postedAt = parseEpochMillis(job.path("createdAt"));

        return new Posting(
                PostingId.generate(), companyId, atsPostingId, title, location,
                remoteType, department, EmploymentType.UNKNOWN, Salary.empty(),
                url, description, SponsorshipSignal.UNKNOWN, postedAt, firstSeenAt, job.toString());
    }

    private static RemoteType mapWorkplaceType(String wt) {
        if (wt == null) return RemoteType.UNKNOWN;
        return switch (wt.toLowerCase(Locale.ROOT)) {
            case "remote" -> RemoteType.REMOTE;
            case "hybrid" -> RemoteType.HYBRID;
            case "on-site", "onsite" -> RemoteType.ONSITE;
            default -> RemoteType.UNKNOWN;
        };
    }

    private static Instant parseEpochMillis(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull() || !node.canConvertToLong()) {
            return null; // honest: no usable timestamp -> no postedAt
        }
        long millis = node.asLong();
        return millis > 0 ? Instant.ofEpochMilli(millis) : null;
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
