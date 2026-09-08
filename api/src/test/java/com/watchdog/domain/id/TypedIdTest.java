package com.watchdog.domain.id;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TypedIdTest {

    @Test
    void generateProducesDistinctIds() {
        assertThat(CompanyId.generate()).isNotEqualTo(CompanyId.generate());
    }

    @Test
    void ofUuidRoundTrips() {
        UUID raw = UUID.randomUUID();
        assertThat(PostingId.of(raw).value()).isEqualTo(raw);
    }

    @Test
    void ofStringParsesUuid() {
        UUID raw = UUID.randomUUID();
        assertThat(UserId.of(raw.toString()).value()).isEqualTo(raw);
    }

    @Test
    void toStringIsTheUuidText() {
        UUID raw = UUID.randomUUID();
        assertThat(FilterProfileId.of(raw)).hasToString(raw.toString());
    }

    @Test
    void equalityIsValueBased() {
        UUID raw = UUID.randomUUID();
        assertThat(JobStateId.of(raw)).isEqualTo(JobStateId.of(raw));
    }

    @Test
    void nullValueRejected() {
        assertThatThrownBy(() -> new CompanyId(null))
                .isInstanceOf(NullPointerException.class);
    }
}
