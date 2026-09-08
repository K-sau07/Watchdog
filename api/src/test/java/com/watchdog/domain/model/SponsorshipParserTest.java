package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SponsorshipParserTest {

    @Test
    void offeredSignals() {
        assertThat(parse("We offer visa sponsorship for this role."))
                .isEqualTo(SponsorshipSignal.OFFERED);
        assertThat(parse("Visa sponsorship available."))
                .isEqualTo(SponsorshipSignal.OFFERED);
        assertThat(parse("We will sponsor qualified candidates."))
                .isEqualTo(SponsorshipSignal.OFFERED);
        assertThat(parse("H1B sponsorship available for the right person."))
                .isEqualTo(SponsorshipSignal.OFFERED);
        assertThat(parse("We provide immigration support."))
                .isEqualTo(SponsorshipSignal.OFFERED);
    }

    @Test
    void notOfferedSignals() {
        assertThat(parse("Must be a US citizen."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
        assertThat(parse("US citizenship required."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
        assertThat(parse("Active security clearance required."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
        assertThat(parse("No visa sponsorship is available for this position."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
        assertThat(parse("We are unable to sponsor visas at this time."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
        assertThat(parse("This role does not offer sponsorship."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
    }

    // --- the conflict rule: NOT_OFFERED wins ---

    @Test
    void notOfferedWinsOverOfferedOnConflict() {
        // A hard citizenship requirement dominates boilerplate sponsorship language.
        String body = "Visa sponsorship available. However, applicants must be a US citizen.";
        assertThat(parse(body)).isEqualTo(SponsorshipSignal.NOT_OFFERED);
    }

    @Test
    void negatedSponsorshipIsNotOffered() {
        // "do not offer ... sponsorship" must not read as OFFERED via a bare "sponsorship".
        assertThat(parse("We do not offer visa sponsorship."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
    }

    // --- honest unknown (the common case) ---

    @Test
    void unknownWhenNoSignal() {
        assertThat(parse("Join our team building great products."))
                .isEqualTo(SponsorshipSignal.UNKNOWN);
        assertThat(parse(null)).isEqualTo(SponsorshipSignal.UNKNOWN);
        assertThat(parse("")).isEqualTo(SponsorshipSignal.UNKNOWN);
    }

    @Test
    void authorizedToWorkAloneIsUnknownNotNotOffered() {
        // "must be authorized to work" without a "without sponsorship" clause does not
        // preclude sponsorship — the honest heuristic answer is UNKNOWN.
        assertThat(parse("Applicants must be authorized to work in the United States."))
                .isEqualTo(SponsorshipSignal.UNKNOWN);
    }

    @Test
    void searchesTitleAndBody() {
        assertThat(SponsorshipParser.parse("Software Engineer (US Citizens Only)", "Great role."))
                .isEqualTo(SponsorshipSignal.NOT_OFFERED);
    }

    /** Body-only convenience for the single-argument tests above. */
    private static SponsorshipSignal parse(String description) {
        return SponsorshipParser.parse(null, description);
    }
}
