package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.JobStateService;
import com.watchdog.application.matching.SingleUser;
import com.watchdog.domain.id.JobStateId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(JobStateController.class)
class JobStateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JobStateService jobStates;

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    private JobStateRecord record(PostingId postingId, JobState state, Instant appliedAt, String note) {
        return new JobStateRecord(JobStateId.generate(), SingleUser.ID, postingId,
                state, appliedAt, note, NOW);
    }

    @Test
    void savedTransitionReturnsPersistedState() throws Exception {
        PostingId id = PostingId.generate();
        when(jobStates.setState(any(), eq(JobState.SAVED), isNull()))
                .thenReturn(record(id, JobState.SAVED, null, null));

        mockMvc.perform(put("/api/postings/" + id.value() + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"SAVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("SAVED"))
                .andExpect(jsonPath("$.postingId").value(id.value().toString()));
    }

    @Test
    void appliedTransitionSurfacesAppliedAtAndNote() throws Exception {
        PostingId id = PostingId.generate();
        when(jobStates.setState(any(), eq(JobState.APPLIED), eq("done")))
                .thenReturn(record(id, JobState.APPLIED, NOW, "done"));

        mockMvc.perform(put("/api/postings/" + id.value() + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"APPLIED\",\"note\":\"done\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state").value("APPLIED"))
                .andExpect(jsonPath("$.appliedAt").exists())
                .andExpect(jsonPath("$.note").value("done"));
    }

    @Test
    void unknownPostingReturns404() throws Exception {
        PostingId id = PostingId.generate();
        when(jobStates.setState(any(), any(), any()))
                .thenThrow(new JobStateService.PostingNotFound(id));

        mockMvc.perform(put("/api/postings/" + id.value() + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"SAVED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidStateReturns400() throws Exception {
        mockMvc.perform(put("/api/postings/" + PostingId.generate().value() + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"MAYBE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void missingStateReturns400() throws Exception {
        mockMvc.perform(put("/api/postings/" + PostingId.generate().value() + "/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\":\"no state here\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void badIdReturns400() throws Exception {
        mockMvc.perform(put("/api/postings/not-a-uuid/state")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"state\":\"SAVED\"}"))
                .andExpect(status().isBadRequest());
    }
}
