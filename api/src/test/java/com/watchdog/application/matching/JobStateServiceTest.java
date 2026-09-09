package com.watchdog.application.matching;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.JobState;
import com.watchdog.domain.model.JobStateRecord;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Salary;
import com.watchdog.domain.model.SponsorshipSignal;
import com.watchdog.domain.port.JobStateRepository;
import com.watchdog.domain.port.PostingRepository;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class JobStateServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-08T12:00:00Z");
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    private final FakeJobStateRepo jobStates = new FakeJobStateRepo();
    private final FakePostingRepo postings = new FakePostingRepo();

    private JobStateService service() {
        return new JobStateService(jobStates, postings, clock);
    }

    // --- in-memory fakes ---

    private static class FakeJobStateRepo implements JobStateRepository {
        final Map<PostingId, JobStateRecord> byPosting = new HashMap<>();
        int saves = 0;
        @Override public JobStateRecord save(JobStateRecord r) {
            byPosting.put(r.postingId(), r); saves++; return r;
        }
        @Override public Optional<JobStateRecord> findByUserAndPosting(UserId u, PostingId p) {
            return Optional.ofNullable(byPosting.get(p));
        }
    }

    private static class FakePostingRepo implements PostingRepository {
        final List<Posting> store = new ArrayList<>();
        @Override public Posting save(Posting p) { store.add(p); return p; }
        @Override public Optional<Posting> findById(PostingId id) {
            return store.stream().filter(p -> p.id().equals(id)).findFirst();
        }
        @Override public Optional<Posting> findByNaturalKey(CompanyId c, String a) { return Optional.empty(); }
        @Override public boolean existsByNaturalKey(CompanyId c, String a) { return false; }
        @Override public List<Posting> findByCompany(CompanyId c) { return List.of(); }
        @Override public List<Posting> findRecent(int limit) { return List.of(); }
        @Override public List<Posting> findSeenSince(java.time.Instant since) { return List.of(); }
        @Override public int deleteStalePostedBefore(java.time.Instant cutoff) { return 0; }
    }

    private PostingId seedPosting() {
        Posting p = new Posting(PostingId.generate(), CompanyId.generate(), "ats-1", "SWE",
                "NYC", RemoteType.REMOTE, null, EmploymentType.FULL_TIME, Salary.empty(),
                "https://x/y", "desc", SponsorshipSignal.UNKNOWN, null, NOW, null);
        postings.save(p);
        return p.id();
    }

    // --- tests ---

    @Test
    void createsJobStateOnFirstTransition() {
        PostingId id = seedPosting();
        JobStateRecord r = service().setState(id, JobState.SAVED, null);

        assertThat(r.state()).isEqualTo(JobState.SAVED);
        assertThat(r.userId()).isEqualTo(SingleUser.ID);
        assertThat(r.postingId()).isEqualTo(id);
        assertThat(jobStates.byPosting).containsKey(id);
    }

    @Test
    void updatesExistingRecordPreservingItsId() {
        PostingId id = seedPosting();
        JobStateRecord first = service().setState(id, JobState.SAVED, null);
        JobStateRecord second = service().setState(id, JobState.APPLIED, null);

        // same row (id preserved) — an upsert, not a second insert
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.state()).isEqualTo(JobState.APPLIED);
        assertThat(jobStates.byPosting).hasSize(1);
    }

    @Test
    void appliedStampsAppliedAt() {
        PostingId id = seedPosting();
        JobStateRecord r = service().setState(id, JobState.APPLIED, null);
        assertThat(r.appliedAt()).isEqualTo(NOW);
    }

    @Test
    void hiddenDoesNotStampAppliedAt() {
        PostingId id = seedPosting();
        JobStateRecord r = service().setState(id, JobState.HIDDEN, null);
        assertThat(r.appliedAt()).isNull();
    }

    @Test
    void nonNullNoteIsStoredNullNoteLeavesExistingUntouched() {
        PostingId id = seedPosting();
        service().setState(id, JobState.SAVED, "follow up Monday");
        JobStateRecord withNote = jobStates.byPosting.get(id);
        assertThat(withNote.note()).isEqualTo("follow up Monday");

        // a later transition with null note preserves the earlier note
        JobStateRecord later = service().setState(id, JobState.APPLIED, null);
        assertThat(later.note()).isEqualTo("follow up Monday");
    }

    @Test
    void unknownPostingRaisesPostingNotFound() {
        Throwable t = catchThrowable(
                () -> service().setState(PostingId.generate(), JobState.SAVED, null));
        assertThat(t).isInstanceOf(JobStateService.PostingNotFound.class);
    }
}
