package com.watchdog.persistence;

import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.port.FilterProfileRepository;
import com.watchdog.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FilterProfileRepositoryAdapterTest extends PostgresIntegrationTest {

    @Autowired
    FilterProfileRepository profiles;

    // The V2-seeded single user (D-WD11) satisfies the user_id FK.
    private static final UserId USER =
            UserId.of(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");

    @Test
    void roundTripsAllDimensionsIncludingArraysAndDuration() {
        FilterCriteria c = FilterCriteria.builder()
                .roleKeywords(List.of("software engineer", "swe"))
                .includeKeywords(List.of("python"))
                .excludeKeywords(List.of("senior", "staff"))
                .locations(List.of("new york", "remote"))
                .seniorities(Set.of(Seniority.NEW_GRAD, Seniority.JUNIOR, Seniority.MID))
                .employmentTypes(Set.of(EmploymentType.FULL_TIME))
                .remoteTypes(Set.of(RemoteType.REMOTE))
                .salaryMin(new BigDecimal("130000"))
                .sponsorshipPref(FilterCriteria.SponsorshipPref.HIDE_NOT_OFFERED)
                .postedWithin(Duration.ofHours(1))
                .seenFrom(NOW.minus(7, ChronoUnit.DAYS))
                .seenTo(NOW)
                .build();
        FilterProfile saved = profiles.save(FilterProfile.create(USER, "My Hunt", c, NOW));

        FilterProfile found = profiles.findById(saved.id()).orElseThrow();
        assertThat(found.name()).isEqualTo("My Hunt");
        assertThat(found.userId()).isEqualTo(USER);
        FilterCriteria fc = found.criteria();
        assertThat(fc.roleKeywords()).containsExactly("software engineer", "swe");
        assertThat(fc.excludeKeywords()).containsExactly("senior", "staff");
        assertThat(fc.locations()).containsExactly("new york", "remote");
        assertThat(fc.seniorities()).containsExactlyInAnyOrder(
                Seniority.NEW_GRAD, Seniority.JUNIOR, Seniority.MID);
        assertThat(fc.employmentTypes()).containsExactly(EmploymentType.FULL_TIME);
        assertThat(fc.remoteTypes()).containsExactly(RemoteType.REMOTE);
        assertThat(fc.salaryMin()).isEqualByComparingTo("130000");
        assertThat(fc.sponsorshipPref()).isEqualTo(FilterCriteria.SponsorshipPref.HIDE_NOT_OFFERED);
        assertThat(fc.postedWithin()).isEqualTo(Duration.ofHours(1));
        assertThat(fc.seenTo()).isEqualTo(NOW);
    }

    @Test
    void roundTripsEmptyCriteria() {
        FilterProfile saved = profiles.save(
                FilterProfile.create(USER, "Everything", FilterCriteria.all(), NOW));

        FilterProfile found = profiles.findById(saved.id()).orElseThrow();
        FilterCriteria fc = found.criteria();
        assertThat(fc.roleKeywords()).isEmpty();
        assertThat(fc.seniorities()).isEmpty();
        assertThat(fc.remoteTypes()).isEmpty();
        assertThat(fc.salaryMin()).isNull();
        assertThat(fc.postedWithin()).isNull();
        assertThat(fc.seenFrom()).isNull();
    }

    @Test
    void updatePersistsAndPreservesId() {
        FilterProfile saved = profiles.save(
                FilterProfile.create(USER, "v1", FilterCriteria.all(), NOW));
        FilterCriteria newCriteria = FilterCriteria.builder()
                .roleKeywords(List.of("data engineer")).build();
        FilterProfile updated = saved.update("v2", newCriteria, NOW.plusSeconds(60));
        profiles.save(updated);

        FilterProfile found = profiles.findById(saved.id()).orElseThrow();
        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.name()).isEqualTo("v2");
        assertThat(found.criteria().roleKeywords()).containsExactly("data engineer");
    }

    @Test
    void findByUserReturnsOwnedProfiles() {
        int before = profiles.findByUser(USER).size();
        profiles.save(FilterProfile.create(USER, "P-" + System.nanoTime(), FilterCriteria.all(), NOW));
        assertThat(profiles.findByUser(USER)).hasSize(before + 1);
    }
}
