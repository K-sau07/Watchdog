package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UsLocationClassifierTest {

    @Test
    void keepsExplicitUsPhrases() {
        assertThat(UsLocationClassifier.isUnitedStates("United States")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Remote - USA")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Remote US")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("United States - Remote")).isTrue();
    }

    @Test
    void keepsUsCitiesAndStateAbbreviations() {
        assertThat(UsLocationClassifier.isUnitedStates("San Francisco")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("New York, NY (HQ)")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Foster City, CA")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Mountain View, California")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Seattle, Washington, United States")).isTrue();
    }

    @Test
    void dropsKnownForeignLocations() {
        assertThat(UsLocationClassifier.isUnitedStates("Singapore")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Bengaluru, India")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Tokyo, Japan")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("London")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Dublin, Ireland")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Vancouver, British Columbia, Canada")).isFalse();
    }

    @Test
    void foreignWinsOverRemote() {
        // "Remote Canada" must be dropped, not kept as an ambiguous remote.
        assertThat(UsLocationClassifier.isUnitedStates("Remote Canada")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Canada - Remote (ON, AB, BC, or NS Only)")).isFalse();
    }

    @Test
    void keepsAmbiguousInclusive() {
        // Ambiguous / unknown → keep (D-WD17 inclusive).
        assertThat(UsLocationClassifier.isUnitedStates("Remote")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Hybrid")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Distributed")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates(null)).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("")).isTrue();
    }
}
