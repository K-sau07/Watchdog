package com.watchdog.application.outreach;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.model.Channel;
import com.watchdog.domain.model.EventKind;
import com.watchdog.domain.model.OutreachEvent;
import com.watchdog.domain.model.OutreachStage;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules that decide what the board shows. All of them come from
 * Pipeline/core/STAGES.md, except the per-channel cap, which is a deliberate extension
 * recorded as D-WD26.
 */
class OutreachProjectionTest {

    private static final ContactId C = ContactId.generate();
    private static final Instant T0 = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant NOW = T0.plus(Duration.ofDays(10));

    private static OutreachEvent sent(Channel ch, EventKind kind, Instant when) {
        return OutreachEvent.sent(C, ch, kind, null, when);
    }

    private static OutreachEvent reply(Instant when) {
        return OutreachEvent.reply(C, Channel.LINKEDIN_DM, "sure, happy to chat", when);
    }

    private static OutreachProjection.State state(List<OutreachEvent> events, Instant now) {
        return OutreachProjection.of(events, null, now);
    }

    // --- an invite is not a message ---

    @Test
    void anInviteSpendsNoneOfTheUnansweredBudget() {
        // STAGES.md: "An invite note is not a message. It is an invite. The counter
        // starts after the accept."
        var s = state(List.of(sent(Channel.LINKEDIN_INVITE, EventKind.INVITE, T0)), NOW);

        assertThat(s.unanswered()).isZero();
        assertThat(s.stage()).isEqualTo(OutreachStage.REACHED_OUT);
        assertThat(s.channelsTouched()).containsExactly(Channel.LINKEDIN_INVITE);
    }

    @Test
    void aDmDoesSpendIt() {
        var s = state(List.of(
                sent(Channel.LINKEDIN_INVITE, EventKind.INVITE, T0),
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0.plus(Duration.ofDays(1)))), NOW);

        assertThat(s.unanswered()).isEqualTo(1);
    }

    // --- the per-channel cap ---

    @Test
    void twoChannelsDoNotInstantlyExhaustAPerson() {
        // The whole point of D-WD26: one message on each of two channels is one unanswered
        // per channel, not two against the person, which would have hit the cap before
        // either channel got a follow-up.
        var s = state(List.of(
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0),
                sent(Channel.EMAIL, EventKind.OPENER, T0)), NOW);

        assertThat(s.unansweredPerChannel())
                .containsEntry(Channel.LINKEDIN_DM, 1)
                .containsEntry(Channel.EMAIL, 1);
        assertThat(s.everyChannelExhausted()).isFalse();
        assertThat(s.stage()).isEqualTo(OutreachStage.REACHED_OUT);
    }

    @Test
    void aChannelIsDoneAfterTwoUnansweredOnIt() {
        var s = state(List.of(
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0),
                sent(Channel.LINKEDIN_DM, EventKind.FOLLOW_UP, T0.plus(Duration.ofDays(2)))), NOW);

        assertThat(s.unansweredPerChannel()).containsEntry(Channel.LINKEDIN_DM, 2);
        assertThat(s.followUpDueOn()).isEmpty();
        assertThat(s.everyChannelExhausted()).isTrue();
        assertThat(s.stage()).isEqualTo(OutreachStage.NURTURE);
    }

    @Test
    void thePersonIsOnlyNurtureWhenEveryChannelIsSpent() {
        List<OutreachEvent> events = new ArrayList<>(List.of(
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0),
                sent(Channel.LINKEDIN_DM, EventKind.FOLLOW_UP, T0.plus(Duration.ofDays(2))),
                sent(Channel.EMAIL, EventKind.OPENER, T0.plus(Duration.ofDays(3)))));

        // LinkedIn is spent but email still has budget, so the person is still live.
        assertThat(state(events, NOW).stage()).isEqualTo(OutreachStage.REACHED_OUT);
        assertThat(state(events, NOW).followUpDueOn()).contains(Channel.EMAIL);

        events.add(sent(Channel.EMAIL, EventKind.FOLLOW_UP, T0.plus(Duration.ofDays(5))));
        assertThat(state(events, NOW).stage()).isEqualTo(OutreachStage.NURTURE);
    }

    // --- the two-day clock ---

    @Test
    void nothingIsDueBeforeTwoDaysHavePassed() {
        Instant justUnder = T0.plus(Duration.ofDays(2)).minusSeconds(1);
        assertThat(state(List.of(sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0)), justUnder)
                .followUpDueOn()).isEmpty();
    }

    @Test
    void itIsDueExactlyOnTheBoundary() {
        Instant exactly = T0.plus(Duration.ofDays(2));
        assertThat(state(List.of(sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0)), exactly)
                .followUpDueOn()).contains(Channel.LINKEDIN_DM);
    }

    @Test
    void anUnansweredInviteIsChasedWithADmRatherThanAnotherInvite() {
        // LinkedIn does not allow a second invite, and they may simply not have accepted
        // yet, so the invite and the DM share one budget and the chase goes out as a DM.
        var s = state(List.of(sent(Channel.LINKEDIN_INVITE, EventKind.INVITE, T0)), NOW);
        assertThat(s.followUpDueOn()).contains(Channel.LINKEDIN_DM);
    }

    @Test
    void theColdestChannelIsChasedFirst() {
        var s = state(List.of(
                sent(Channel.EMAIL, EventKind.OPENER, T0),
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0.plus(Duration.ofDays(3)))), NOW);

        assertThat(s.followUpDueOn()).contains(Channel.EMAIL);
    }

    // --- replies ---

    @Test
    void aReplyResetsTheCounterAndNothingIsDue() {
        // The reset is why the cap counts unanswered messages rather than total ones:
        // under a lifetime limit a real conversation became illegal partway through.
        var s = state(List.of(
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0),
                sent(Channel.LINKEDIN_DM, EventKind.FOLLOW_UP, T0.plus(Duration.ofDays(2))),
                reply(T0.plus(Duration.ofDays(3)))), NOW);

        assertThat(s.unanswered()).isZero();
        assertThat(s.turns()).isEqualTo(1);
        assertThat(s.stage()).isEqualTo(OutreachStage.LIVE);
        // The ball is theirs to answer, not ours to chase.
        assertThat(s.followUpDueOn()).isEmpty();
    }

    @Test
    void aSecondRealTurnIsAnExchange() {
        var s = state(List.of(
                sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0),
                reply(T0.plus(Duration.ofDays(1))),
                sent(Channel.LINKEDIN_DM, EventKind.FOLLOW_UP, T0.plus(Duration.ofDays(2))),
                reply(T0.plus(Duration.ofDays(3)))), NOW);

        assertThat(s.turns()).isEqualTo(2);
        assertThat(s.stage()).isEqualTo(OutreachStage.EXCHANGE);
    }

    @Test
    void aLiveConversationHasNoCeiling() {
        List<OutreachEvent> events = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            events.add(sent(Channel.LINKEDIN_DM, EventKind.FOLLOW_UP, T0.plusSeconds(i * 7200L)));
            events.add(reply(T0.plusSeconds(i * 7200L + 3600)));
        }
        var s = state(events, NOW);

        assertThat(s.turns()).isEqualTo(10);
        assertThat(s.unanswered()).isZero();
        assertThat(s.stage()).isEqualTo(OutreachStage.EXCHANGE);
    }

    // --- declared stages and drafts ---

    @Test
    void aDeclaredStageWinsOverTheDerivedOne() {
        List<OutreachEvent> events = List.of(sent(Channel.LINKEDIN_DM, EventKind.OPENER, T0));

        assertThat(OutreachProjection.of(events, OutreachStage.DEAD, NOW).stage())
                .isEqualTo(OutreachStage.DEAD);
        // And a finished thread is never queued for a chase.
        assertThat(OutreachProjection.of(events, OutreachStage.DEAD, NOW).followUpDueOn())
                .isEmpty();
    }

    @Test
    void aDraftSpendsNothingAndStartsNoClock() {
        // Written but not sent has reached nobody.
        var s = state(List.of(OutreachEvent.drafted(C, Channel.EMAIL, EventKind.OPENER, "hi", T0)), NOW);

        assertThat(s.unanswered()).isZero();
        assertThat(s.channelsTouched()).isEmpty();
        assertThat(s.followUpDueOn()).isEqualTo(Optional.empty());
    }

    @Test
    void noEventsMeansNothingHappened() {
        var s = state(List.of(), NOW);

        assertThat(s.stage()).isEqualTo(OutreachStage.REACHED_OUT);
        assertThat(s.turns()).isZero();
        assertThat(s.hasReplied()).isFalse();
        assertThat(s.everyChannelExhausted()).isFalse();
        assertThat(s.followUpDueOn()).isEmpty();
    }
}
