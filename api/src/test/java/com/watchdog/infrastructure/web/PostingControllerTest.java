package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.MatchingService;
import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.model.SponsorshipSignal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PostingController.class)
class PostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MatchingService matching;

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    private MatchingService.MatchedPosting sampleMatch() {
        Posting p = new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-1",
                "Software Engineer, New Grad", "New York, NY", RemoteType.ONSITE, "Eng",
                EmploymentType.FULL_TIME, Salary.of(new BigDecimal("120000"), new BigDecimal("150000"), "USD"),
                "https://x/y", "Great role. Visa sponsorship available.",
                SponsorshipSignal.OFFERED, NOW.minus(Duration.ofMinutes(4)), NOW, null);
        return new MatchingService.MatchedPosting(p, Seniority.NEW_GRAD, AtsSource.GREENHOUSE, "Ramp");
    }

    @Test
    void listReturnsPagedFeedWithDerivedFields() throws Exception {
        var page = new MatchingService.Page(List.of(sampleMatch()), 0, 25, 1);
        when(matching.search(any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/postings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMatched").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Software Engineer, New Grad"))
                .andExpect(jsonPath("$.items[0].companyName").value("Ramp"))
                .andExpect(jsonPath("$.items[0].seniority").value("NEW_GRAD"))
                .andExpect(jsonPath("$.items[0].sponsorshipSignal").value("OFFERED"))
                .andExpect(jsonPath("$.items[0].source").value("GREENHOUSE"))
                .andExpect(jsonPath("$.items[0].salary.min").value(120000))
                .andExpect(jsonPath("$.items[0].caughtMinutes").value(4)); // signature stat
    }

    @Test
    void detailReturnsFullPostingWhenFound() throws Exception {
        MatchingService.MatchedPosting m = sampleMatch();
        when(matching.findEnriched(any())).thenReturn(Optional.of(m));

        mockMvc.perform(get("/api/postings/" + m.posting().id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.title").value("Software Engineer, New Grad"))
                .andExpect(jsonPath("$.description").value("Great role. Visa sponsorship available."));
    }

    @Test
    void detailReturns404WhenUnknown() throws Exception {
        when(matching.findEnriched(any())).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/postings/" + PostingId.generate().value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void badFilterParamReturns400() throws Exception {
        mockMvc.perform(get("/api/postings").param("seniority", "wizard"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void badIdReturns400() throws Exception {
        mockMvc.perform(get("/api/postings/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
