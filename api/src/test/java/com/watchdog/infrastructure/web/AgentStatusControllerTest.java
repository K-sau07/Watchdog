package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.AgentStatusService;
import com.watchdog.application.matching.AgentStatusService.AgentStatus;
import com.watchdog.application.polling.OnDemandPollService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AgentStatusController.class)
class AgentStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AgentStatusService agentStatus;

    @MockitoBean
    private OnDemandPollService onDemandPoll;

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Test
    void statusExposesCountsAndMedianMinutes() throws Exception {
        when(agentStatus.current()).thenReturn(new AgentStatus(
                25, 16, 2, 7, NOW.minus(Duration.ofSeconds(38)),
                NOW.plus(Duration.ofSeconds(82)), 12, Optional.of(Duration.ofMinutes(6))));

        mockMvc.perform(get("/api/agent/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.boardsWatched").value(25))
                .andExpect(jsonPath("$.greenhouse").value(16))
                .andExpect(jsonPath("$.ashby").value(7))
                .andExpect(jsonPath("$.newToday").value(12))
                .andExpect(jsonPath("$.medianCatchMinutesToday").value(6));
    }

    @Test
    void nullsBeforeFirstPollAndUnknownMedian() throws Exception {
        when(agentStatus.current()).thenReturn(new AgentStatus(
                25, 16, 2, 7, null, null, 0, Optional.empty()));

        mockMvc.perform(get("/api/agent/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastPoll").doesNotExist())
                .andExpect(jsonPath("$.nextPoll").doesNotExist())
                .andExpect(jsonPath("$.medianCatchMinutesToday").doesNotExist());
    }

    @Test
    void pollReturns200WithCountsWhenItRuns() throws Exception {
        when(onDemandPoll.refreshNow())
                .thenReturn(new OnDemandPollService.Result.Ran(104, 7));

        mockMvc.perform(post("/api/agent/poll"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("polled"))
                .andExpect(jsonPath("$.companiesPolled").value(104))
                .andExpect(jsonPath("$.newPostings").value(7));
    }

    @Test
    void pollReturns429WhenCoolingDown() throws Exception {
        when(onDemandPoll.refreshNow())
                .thenReturn(new OnDemandPollService.Result.CoolingDown(90));

        mockMvc.perform(post("/api/agent/poll"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value("cooldown"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(90));
    }
}
