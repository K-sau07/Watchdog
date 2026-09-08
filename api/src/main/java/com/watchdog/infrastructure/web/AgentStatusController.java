package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.AgentStatusService;
import com.watchdog.application.matching.AgentStatusService.AgentStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

/**
 * Agent-status endpoint (spec §7 {@code GET /api/agent/status}): powers the dashboard's
 * "live agent" bar + the signature stat (spec §6). Thin over {@link AgentStatusService};
 * all numbers are DB-derived (D-WD12).
 */
@RestController
@RequestMapping("/api/agent")
class AgentStatusController {

    private final AgentStatusService agentStatus;

    AgentStatusController(AgentStatusService agentStatus) {
        this.agentStatus = agentStatus;
    }

    /** Response DTO: null poll times before first poll; null median when unknown today. */
    record StatusResponse(
            int boardsWatched,
            long greenhouse,
            long lever,
            long ashby,
            Instant lastPoll,
            Instant nextPoll,
            int newToday,
            Long medianCatchMinutesToday
    ) {
        static StatusResponse from(AgentStatus s) {
            return new StatusResponse(
                    s.boardsWatched(), s.greenhouse(), s.lever(), s.ashby(),
                    s.lastPoll(), s.nextPoll(), s.newToday(),
                    s.medianCatchTimeToday().map(java.time.Duration::toMinutes).orElse(null));
        }
    }

    @GetMapping("/status")
    public StatusResponse status() {
        return StatusResponse.from(agentStatus.current());
    }
}
