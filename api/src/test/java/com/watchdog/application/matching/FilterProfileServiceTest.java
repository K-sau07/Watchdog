package com.watchdog.application.matching;

import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.port.FilterProfileRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class FilterProfileServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final FakeRepo repo = new FakeRepo();

    private FilterProfileService service() {
        return new FilterProfileService(repo, clock);
    }

    private static class FakeRepo implements FilterProfileRepository {
        final List<FilterProfile> store = new ArrayList<>();
        @Override public FilterProfile save(FilterProfile p) {
            store.removeIf(x -> x.id().equals(p.id()));
            store.add(p);
            return p;
        }
        @Override public Optional<FilterProfile> findById(FilterProfileId id) {
            return store.stream().filter(p -> p.id().equals(id)).findFirst();
        }
        @Override public List<FilterProfile> findByUser(UserId userId) {
            return store.stream().filter(p -> p.userId().equals(userId))
                    .sorted(Comparator.comparing(FilterProfile::updatedAt).reversed())
                    .toList();
        }
    }

    @Test
    void getOrCreateDefaultCreatesForSingleUserWhenNoneExist() {
        FilterProfile p = service().getOrCreateDefault();

        assertThat(p.userId()).isEqualTo(SingleUser.ID);
        assertThat(p.name()).isEqualTo("Default");
        // owner's default hunt: early-career SWE targeting NEW_GRAD/JUNIOR/MID
        assertThat(p.criteria().roleKeywords()).contains("software engineer");
        assertThat(p.criteria().seniorities())
                .containsExactlyInAnyOrder(Seniority.NEW_GRAD, Seniority.JUNIOR, Seniority.MID);
        assertThat(repo.store).hasSize(1);
    }

    @Test
    void getOrCreateDefaultIsIdempotent() {
        FilterProfileService svc = service();
        FilterProfile first = svc.getOrCreateDefault();
        FilterProfile second = svc.getOrCreateDefault();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(repo.store).hasSize(1); // not re-created
    }

    @Test
    void defaultDoesNotExcludeOnExperienceFloor() {
        // Owner has 2+ yrs and clears a ~2-year floor; the default must not exclude such roles.
        FilterProfile p = service().getOrCreateDefault();
        assertThat(p.criteria().excludeKeywords()).isEmpty();
        assertThat(p.criteria().seniorities()).contains(Seniority.MID);
    }

    @Test
    void updateChangesNameAndCriteriaPreservingId() {
        FilterProfile created = service().getOrCreateDefault();
        FilterCriteria newCriteria = FilterCriteria.builder()
                .roleKeywords(List.of("data engineer")).build();

        FilterProfile updated = service().update(created.id(), "Data Hunt", newCriteria);

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Data Hunt");
        assertThat(updated.criteria().roleKeywords()).containsExactly("data engineer");
        assertThat(updated.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void getByIdUnknownThrows() {
        Throwable t = catchThrowable(() -> service().getById(FilterProfileId.generate()));
        assertThat(t).isInstanceOf(FilterProfileService.ProfileNotFound.class);
    }

    @Test
    void updateUnknownThrows() {
        Throwable t = catchThrowable(() -> service().update(
                FilterProfileId.generate(), "x", FilterCriteria.all()));
        assertThat(t).isInstanceOf(FilterProfileService.ProfileNotFound.class);
    }
}
