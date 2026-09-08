package com.watchdog.application.polling;

import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Posting;
import com.watchdog.domain.port.JobSourcePort;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AtsSourceRouterTest {

    /** Minimal fake port that serves one source and returns nothing. */
    private static JobSourcePort fakePort(AtsSource source) {
        return new JobSourcePort() {
            @Override public AtsSource source() { return source; }
            @Override public List<Posting> fetchPostings(Company company) { return List.of(); }
        };
    }

    @Test
    void routesEachSourceToItsPort() {
        JobSourcePort gh = fakePort(AtsSource.GREENHOUSE);
        JobSourcePort lever = fakePort(AtsSource.LEVER);
        AtsSourceRouter router = new AtsSourceRouter(List.of(gh, lever));

        assertThat(router.routeTo(AtsSource.GREENHOUSE)).isSameAs(gh);
        assertThat(router.routeTo(AtsSource.LEVER)).isSameAs(lever);
    }

    @Test
    void supportedSourcesReflectsRegisteredPorts() {
        AtsSourceRouter router = new AtsSourceRouter(
                List.of(fakePort(AtsSource.GREENHOUSE), fakePort(AtsSource.ASHBY)));
        assertThat(router.supportedSources())
                .containsExactlyInAnyOrder(AtsSource.GREENHOUSE, AtsSource.ASHBY);
    }

    @Test
    void missingSourceThrows() {
        AtsSourceRouter router = new AtsSourceRouter(List.of(fakePort(AtsSource.GREENHOUSE)));
        assertThatThrownBy(() -> router.routeTo(AtsSource.LEVER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("LEVER");
    }

    @Test
    void duplicateSourceIsRejected() {
        assertThatThrownBy(() -> new AtsSourceRouter(
                List.of(fakePort(AtsSource.GREENHOUSE), fakePort(AtsSource.GREENHOUSE))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GREENHOUSE");
    }
}
