package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.JobStateService;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * The daily-workflow write endpoint (spec §7): {@code PUT /api/postings/{id}/state}
 * sets the single user's SAVED / APPLIED / HIDDEN state (+ optional note) for a posting.
 * Thin controller over {@link JobStateService}; single-user (D-WD5).
 */
@RestController
@RequestMapping("/api/postings")
class JobStateController {

    private final JobStateService jobStates;

    JobStateController(JobStateService jobStates) {
        this.jobStates = jobStates;
    }

    /** Request body: the target state and an optional note. */
    record StateUpdate(String state, String note) {
    }

    /** Response: the persisted state for this posting. */
    record StateResponse(String postingId, String state, Instant appliedAt, String note,
                         Instant updatedAt) {
        static StateResponse from(JobStateRecord r) {
            return new StateResponse(
                    r.postingId().value().toString(), r.state().name(),
                    r.appliedAt(), r.note(), r.updatedAt());
        }
    }

    @PutMapping("/{id}/state")
    public StateResponse setState(@PathVariable String id, @RequestBody StateUpdate body) {
        PostingId postingId = parseId(id);
        JobState state = parseState(body == null ? null : body.state());
        String note = body == null ? null : body.note();
        return StateResponse.from(jobStates.setState(postingId, state, note));
    }

    private static PostingId parseId(String raw) {
        try {
            return PostingId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new BadRequest("invalid posting id: '" + raw + "'");
        }
    }

    private static JobState parseState(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequest("state is required (SAVED|APPLIED|HIDDEN|NEW)");
        }
        try {
            return JobState.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new BadRequest("invalid state: '" + raw + "' (SAVED|APPLIED|HIDDEN|NEW)");
        }
    }

    /** Client error for a malformed body/id. */
    static final class BadRequest extends RuntimeException {
        BadRequest(String message) {
            super(message);
        }
    }

    @ExceptionHandler(BadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onBadRequest(BadRequest e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(JobStateService.PostingNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> onNotFound(JobStateService.PostingNotFound e) {
        return Map.of("error", e.getMessage());
    }
}
