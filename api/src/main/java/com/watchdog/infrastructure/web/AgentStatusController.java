package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.AgentStatusService;
import com.watchdog.application.matching.AgentStatusService.AgentStatus;
import com.watchdog.application.polling.OnDemandPollService;
import com.watchdog.application.polling.OnDemandPollService.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

/**
 * Agent endpoints (spec §7): {@code GET /api/agent/status} powers the dashboard bar +
 * signature stat (DB-derived, D-WD12); {@code POST /api/agent/poll} triggers an on-demand
 * refresh with a cooldown (D-WD19). Thin over the application services.
 */
@RestController
@RequestMapping("/api/agent")
class AgentStatusController {

    private final AgentStatusService agentStatus;
    private final OnDemandPollService onDemandPoll;

    AgentStatusController(AgentStatusService agentStatus, OnDemandPollService onDemandPoll) {
        this.agentStatus = agentStatus;
        this.onDemandPoll = onDemandPoll;
    }

    /**
     * Response DTO: null poll times before first poll; null median when nothing was
     * genuinely caught today.
     *
     * <p>{@code newToday} and {@code caughtToday} are different numbers and the client
     * must be able to tell them apart. The first counts every posting written today,
     * backfill included. The second counts only those posted after we started watching
     * their board — the population the median is computed over. When a big backfill
     * lands, the first is large and the second can be zero, and reporting one figure
     * would make the median look like it describes all of them.
     */
    record StatusResponse(
            int boardsWatched,
            long greenhouse,
            long lever,
            long ashby,
            Instant lastPoll,
            Instant nextPoll,
            int newToday,
            Long medianCatchMinutesToday,
            int caughtToday
    ) {
        static StatusResponse from(AgentStatus s) {
            return new StatusResponse(
                    s.boardsWatched(), s.greenhouse(), s.lever(), s.ashby(),
                    s.lastPoll(), s.nextPoll(), s.newToday(),
                    s.medianCatchTimeToday().map(java.time.Duration::toMinutes).orElse(null),
                    s.caughtToday());
        }
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return StatusResponse.from(agentStatus.current());
    }

    /**
     * On-demand refresh (D-WD19). 200 with counts if it ran; 429 with retryAfterSeconds if
     * the cooldown hasn't elapsed — so rapid clicks can't recreate a request burst.
     */
    @PostMapping("/poll")
    public ResponseEntity<Map<String, Object>> poll() {
        Result result = onDemandPoll.refreshNow();
        if (result instanceof Result.Ran ran) {
            return ResponseEntity.ok(Map.of(
                    "status", "polled",
                    "companiesPolled", ran.companiesPolled(),
                    "newPostings", ran.newPostings()));
        }
        Result.CoolingDown cd = (Result.CoolingDown) result;
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(cd.retryAfterSeconds()))
                .body(Map.of(
                        "status", "cooldown",
                        "retryAfterSeconds", cd.retryAfterSeconds()));
    }
}
