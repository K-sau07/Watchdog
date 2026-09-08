package com.watchdog.domain.model;

import com.watchdog.domain.id.CompanyId;
import com.watchdog.domain.id.PostingId;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostingTest {

    private Posting posting(String title, Instant postedAt, Instant firstSeenAt) {
        return new Posting(
                PostingId.generate(), CompanyId.generate(), "ats-1", title,
                "Remote", RemoteType.REMOTE, null, null, null,
                "https://x/y", "desc", null, postedAt, firstSeenAt, null);
    }

    @Test
    void catchTimeEmptyWhenNoPostedAt() {
        Posting p = posting("SWE", null, Instant.now());
        assertThat(p.catchTime()).isEmpty();
        assertThat(p.catchMinutes()).isEmpty();
    }

    @Test
    void catchTimeIsDeltaWhenPostedAtKnown() {
        Instant posted = Instant.parse("2026-01-01T10:00:00Z");
        Instant seen = Instant.parse("2026-01-01T10:04:00Z");
        Posting p = posting("SWE", posted, seen);
        assertThat(p.catchTime()).contains(Duration.ofMinutes(4));
        assertThat(p.catchMinutes()).contains(4L);
    }

    @Test
    void catchTimeClampedToZeroOnClockSkew() {
        Instant posted = Instant.parse("2026-01-01T10:05:00Z");
        Instant seen = Instant.parse("2026-01-01T10:00:00Z"); // seen "before" posted
        Posting p = posting("SWE", posted, seen);
        assertThat(p.catchTime()).contains(Duration.ZERO);
        assertThat(p.catchMinutes()).contains(0L);
    }

    @Test
    void nullableDomainFieldsDefaultToUnknownAndEmptySalary() {
        // employmentType/sponsorship/salary passed as null above -> defaulted.
        Posting p = posting("SWE", null, Instant.now());
        assertThat(p.remoteType()).isEqualTo(RemoteType.REMOTE);
        assertThat(p.employmentType()).isEqualTo(EmploymentType.UNKNOWN);
        assertThat(p.sponsorshipSignal()).isEqualTo(SponsorshipSignal.UNKNOWN);
        assertThat(p.salary()).isEqualTo(Salary.empty());
        assertThat(p.salary().isEmpty()).isTrue();
    }

    @Test
    void blankTitleRejected() {
        assertThatThrownBy(() -> posting("  ", null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void firstSeenAtRequired() {
        assertThatThrownBy(() -> posting("SWE", null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
