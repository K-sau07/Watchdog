package com.watchdog.application.polling;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.port.PostingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class RetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private static class CapturingRepo implements PostingRepository {
        Instant cutoffSeen;
        int toReturn;
        @Override public int deleteStalePostedBefore(Instant cutoff) { cutoffSeen = cutoff; return toReturn; }
        // unused
        @Override public Posting save(Posting p) { return p; }
        @Override public Optional<Posting> findById(PostingId id) { return Optional.empty(); }
        @Override public Optional<Posting> findByNaturalKey(CompanyId c, String a) { return Optional.empty(); }
        @Override public boolean existsByNaturalKey(CompanyId c, String a) { return false; }
        @Override public List<Posting> findByCompany(CompanyId c) { return List.of(); }
        @Override public List<Posting> findRecent(int limit) { return List.of(); }
        @Override public List<Posting> findSeenSince(Instant since) { return List.of(); }
    }

    @Test
    void prunesUsingConfiguredWindowFromNow() {
        CapturingRepo repo = new CapturingRepo();
        repo.toReturn = 42;
        RetentionService svc = new RetentionService(repo, clock, 21);

        int deleted = svc.prune();

        assertThat(deleted).isEqualTo(42);
        assertThat(repo.cutoffSeen).isEqualTo(NOW.minus(Duration.ofDays(21)));
    }

    @Test
    void respectsACustomWindow() {
        CapturingRepo repo = new CapturingRepo();
        RetentionService svc = new RetentionService(repo, clock, 7);
        svc.prune();
        assertThat(repo.cutoffSeen).isEqualTo(NOW.minus(Duration.ofDays(7)));
    }
}
