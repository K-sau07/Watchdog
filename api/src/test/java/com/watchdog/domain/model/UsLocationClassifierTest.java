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
    void usSignalWinsWhenBothPresent() {
        // A multi-location role that includes the US IS a US role — don't drop it for Canada.
        assertThat(UsLocationClassifier.isUnitedStates("Remote, Canada; Remote, United States")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Remote, Canada; Remote, US")).isTrue();
    }

    @Test
    void keepsBareUsAndStateNamesAndNorthAmerica() {
        assertThat(UsLocationClassifier.isUnitedStates("US")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("US - Remote")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("North America")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Remote - California")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("McLean, Virginia")).isTrue();
        assertThat(UsLocationClassifier.isUnitedStates("Remote - Texas")).isTrue();
    }

    @Test
    void dropsForeignPlacesTheFirstPassMissed() {
        // These leaked into the US view before the audit-driven expansion.
        assertThat(UsLocationClassifier.isUnitedStates("Belgrade, Serbia")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("São Paulo")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Zürich, Switzerland")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Reykjavík")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Aarhus, Denmark")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Remote UK")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Europe")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Mexico City")).isFalse();
        assertThat(UsLocationClassifier.isUnitedStates("Ljubljana, Slovenia")).isFalse();
    }

    @Test
    void newMexicoIsUsNotMexico() {
        // "New Mexico" is a US state; must not be caught by the Mexico-City foreign rule.
        assertThat(UsLocationClassifier.isUnitedStates("Albuquerque, New Mexico")).isTrue();
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
