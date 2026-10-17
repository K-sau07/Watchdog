package com.watchdog.domain.model;

import com.watchdog.domain.id.UserId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Contact is identity and routing only. The cap and stage rules it used to carry live in
 * {@link com.watchdog.application.outreach.OutreachProjection} now, computed from events
 * — see OutreachProjectionTest.
 */
class ContactTest {

    private static final UserId USER = UserId.of(UUID.randomUUID());
    private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");

    private static Contact target() {
        return Contact.target(USER, "Ramp", "Dana Lee", "Engineering Manager",
                "https://linkedin.com/in/dana", null, PersonType.HIRING_MANAGER, null, T0);
    }

    @Test
    void aNewTargetHasNoDeclaredStage() {
        Contact c = target();
        assertThat(c.stageOverride()).isNull();
        assertThat(c.personType()).isEqualTo(PersonType.HIRING_MANAGER);
        assertThat(c.isEmailable()).isFalse();
    }

    @Test
    void anAddressMakesTheEmailChannelUsable() {
        assertThat(target().withEmail("dana@ramp.com").isEmailable()).isTrue();
        // Blank is not an address. A whitespace string would otherwise queue an email to
        // nobody and read as covered on the board.
        assertThat(target().withEmail("   ").isEmailable()).isFalse();
    }

    @Test
    void onlyDeclarableStagesCanBeDeclared() {
        // These four cannot be derived from events: nothing in a log says "they sounded
        // irritated" or "the ask was made".
        for (OutreachStage s : new OutreachStage[]{
                OutreachStage.ASKED, OutreachStage.WON, OutreachStage.DEAD, OutreachStage.NURTURE}) {
            assertThat(Contact.isDeclarable(s)).isTrue();
            assertThat(target().withStageOverride(s).stageOverride()).isEqualTo(s);
        }
    }

    @Test
    void derivedStagesAreRejectedAsDeclarations() {
        // Declaring one of these would let the board disagree with its own event log.
        assertThatThrownBy(() -> target().withStageOverride(OutreachStage.EXCHANGE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target().withStageOverride(OutreachStage.LIVE))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target().withStageOverride(OutreachStage.REACHED_OUT))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aContactMustNameBothAHumanAndACompany() {
        assertThatThrownBy(() -> Contact.target(USER, "  ", "Dana Lee", null, null, null, null, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Contact.target(USER, "Ramp", "", null, null, null, null, null, T0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void personTypeMirrorsTheVaultVocabulary() {
        // Same five values as the `type` field in Pipeline/people/_TEMPLATE.md, so the
        // app and the vault never drift into two classification schemes.
        assertThat(PersonType.values()).containsExactly(
                PersonType.ENGINEER, PersonType.RECRUITER, PersonType.HIRING_MANAGER,
                PersonType.ALUM, PersonType.SENIOR_OFF_REQ);
    }
}
