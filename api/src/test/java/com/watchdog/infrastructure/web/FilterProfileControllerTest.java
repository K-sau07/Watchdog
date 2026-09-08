package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.FilterProfileService;
import com.watchdog.application.matching.SingleUser;
import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.Seniority;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FilterProfileController.class)
class FilterProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FilterProfileService profiles;

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    private FilterProfile defaultProfile() {
        FilterCriteria c = FilterCriteria.builder()
                .roleKeywords(List.of("software engineer", "swe"))
                .seniorities(Set.of(Seniority.NEW_GRAD, Seniority.JUNIOR, Seniority.MID))
                .build();
        return FilterProfile.create(SingleUser.ID, "Default", c, NOW);
    }

    @Test
    void getDefaultReturnsAutoProvisionedProfile() throws Exception {
        when(profiles.getOrCreateDefault()).thenReturn(defaultProfile());

        mockMvc.perform(get("/api/filter-profiles/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Default"))
                .andExpect(jsonPath("$.roleKeywords[0]").value("software engineer"))
                .andExpect(jsonPath("$.seniorities").isArray());
    }

    @Test
    void getByIdReturnsProfile() throws Exception {
        FilterProfile p = defaultProfile();
        when(profiles.getById(any())).thenReturn(p);

        mockMvc.perform(get("/api/filter-profiles/" + p.id().value()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(p.id().value().toString()));
    }

    @Test
    void getUnknownIdReturns404() throws Exception {
        FilterProfileId id = FilterProfileId.generate();
        when(profiles.getById(any())).thenThrow(new FilterProfileService.ProfileNotFound(id));

        mockMvc.perform(get("/api/filter-profiles/" + id.value()))
                .andExpect(status().isNotFound());
    }

    @Test
    void putUpdatesProfile() throws Exception {
        FilterProfile p = defaultProfile();
        when(profiles.update(any(), eq("Data Hunt"), any())).thenReturn(
                p.update("Data Hunt",
                        FilterCriteria.builder().roleKeywords(List.of("data engineer")).build(), NOW));

        mockMvc.perform(put("/api/filter-profiles/" + p.id().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Data Hunt\",\"roleKeywords\":[\"data engineer\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Data Hunt"))
                .andExpect(jsonPath("$.roleKeywords[0]").value("data engineer"));
    }

    @Test
    void putWithoutNameReturns400() throws Exception {
        mockMvc.perform(put("/api/filter-profiles/" + FilterProfileId.generate().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleKeywords\":[\"x\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putWithInvalidEnumReturns400() throws Exception {
        mockMvc.perform(put("/api/filter-profiles/" + FilterProfileId.generate().value())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"seniorities\":[\"wizard\"]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void badIdReturns400() throws Exception {
        mockMvc.perform(get("/api/filter-profiles/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}
