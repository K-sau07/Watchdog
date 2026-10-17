package com.watchdog.application.outreach;

import com.watchdog.domain.model.Channel;
import com.watchdog.domain.model.OutreachEvent;
import com.watchdog.domain.model.OutreachEvent.Direction;
import com.watchdog.domain.model.OutreachStage;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Everything a contact's state can be worked out from its event log.
 *
 * <p>Pure and static on purpose: no repository, no clock field, nothing to stub. The
 * counters this computes — stage, turns, unanswered — are also written in the frontmatter
 * of Pipeline/people/*.md, and keeping two independently-maintained copies of one fact is
 * what produced forty-five rows of stale state in the system that vault replaced. Here
 * only the events are stored and these values are derived, so they cannot drift.
 */
public final class OutreachProjection {

    /** Chase something that went quiet after this long. */
    public static final Duration FOLLOW_UP_AFTER = Duration.ofDays(2);

    /**
     * Two unanswered messages on one channel and that channel is done.
     *
     * <p>STAGES.md caps unanswered messages at two per person. That cap is applied here
     * per channel instead, because a person hit on LinkedIn and by email would otherwise
     * be at the cap before either channel got a single follow-up, which defeats the point
     * of using two channels at all. A person is only finished when every channel is.
     */
    public static final int UNANSWERED_CAP_PER_CHANNEL = 2;

    private OutreachProjection() {
    }

    /** The computed state of one contact. */
    public record State(
            OutreachStage stage,
            int turns,
            int unanswered,
            Set<Channel> channelsTouched,
            Map<Channel, Integer> unansweredPerChannel,
            Instant lastOutboundAt,
            Instant lastInboundAt,
            /** The channel to chase on, when one is due. */
            Optional<Channel> followUpDueOn
    ) {
        /** They have written something real at some point. */
        public boolean hasReplied() {
            return turns > 0;
        }

        public boolean everyChannelExhausted() {
            return !channelsTouched.isEmpty()
                    && unansweredPerChannel.values().stream()
                            .allMatch(n -> n >= UNANSWERED_CAP_PER_CHANNEL);
        }
    }

    /**
     * Replay a contact's events into its current state.
     *
     * @param events  every event for one contact, in any order
     * @param declared a stage a human stated (ASKED/WON/DEAD/NURTURE), or null to derive
     */
    public static State of(List<OutreachEvent> events, OutreachStage declared, Instant now) {
        List<OutreachEvent> ordered = events.stream()
                .sorted(Comparator.comparing(OutreachEvent::occurredAt))
                .toList();

        Instant lastInbound = ordered.stream()
                .filter(e -> e.direction() == Direction.INBOUND)
                .map(OutreachEvent::occurredAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        Instant lastOutbound = ordered.stream()
                .filter(OutreachEvent::isSentOutbound)
                .map(OutreachEvent::occurredAt)
                .max(Comparator.naturalOrder())
                .orElse(null);

        int turns = (int) ordered.stream().filter(e -> e.direction() == Direction.INBOUND).count();

        Set<Channel> touched = ordered.stream()
                .filter(OutreachEvent::isSentOutbound)
                .map(OutreachEvent::channel)
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(Channel.class)));

        // Unanswered is counted since their last reply, because a reply resets it. That
        // reset is the entire reason the cap counts unanswered messages rather than total
        // ones — under a lifetime limit a real conversation became illegal partway through.
        Map<Channel, Integer> unansweredPer = new EnumMap<>(Channel.class);
        for (OutreachEvent e : ordered) {
            if (!e.countsAsUnanswered()) {
                continue;
            }
            if (lastInbound != null && !e.occurredAt().isAfter(lastInbound)) {
                continue;
            }
            // An invite is chased with a DM, so both land in the same channel budget.
            unansweredPer.merge(e.channel().followUpChannel(), 1, Integer::sum);
        }

        // A channel that has only ever carried an unanswered invite still has its full
        // budget: the invite spent nothing, but the channel is live and chaseable.
        for (Channel c : touched) {
            unansweredPer.putIfAbsent(c.followUpChannel(), 0);
        }

        OutreachStage stage = declared != null
                ? declared
                : derive(turns, touched, unansweredPer);

        return new State(
                stage,
                turns,
                unansweredPer.values().stream().mapToInt(Integer::intValue).max().orElse(0),
                Set.copyOf(touched),
                Map.copyOf(unansweredPer),
                lastOutbound,
                lastInbound,
                followUpDueOn(ordered, unansweredPer, lastInbound, stage, now));
    }

    private static OutreachStage derive(int turns, Set<Channel> touched,
                                        Map<Channel, Integer> unansweredPer) {
        if (turns >= 2) {
            return OutreachStage.EXCHANGE;
        }
        if (turns == 1) {
            return OutreachStage.LIVE;
        }
        boolean exhausted = !touched.isEmpty()
                && unansweredPer.values().stream().allMatch(n -> n >= UNANSWERED_CAP_PER_CHANNEL);
        return exhausted ? OutreachStage.NURTURE : OutreachStage.REACHED_OUT;
    }

    /**
     * Which channel is owed a chase, if any.
     *
     * <p>Nothing is due while the ball is theirs to return — if they replied after your
     * last message then you owe them an answer, which is a different thing from a chase
     * and is reported separately. Nothing is due on a channel at its cap, and nothing is
     * due on a thread that has been declared finished.
     *
     * <p>When several channels qualify, the coldest one wins: the longest silence is the
     * one most worth breaking.
     */
    private static Optional<Channel> followUpDueOn(List<OutreachEvent> ordered,
                                                   Map<Channel, Integer> unansweredPer,
                                                   Instant lastInbound,
                                                   OutreachStage stage,
                                                   Instant now) {
        if (stage == OutreachStage.NURTURE || stage == OutreachStage.DEAD
                || stage == OutreachStage.WON) {
            return Optional.empty();
        }

        Map<Channel, Instant> lastSendPerChannel = new EnumMap<>(Channel.class);
        for (OutreachEvent e : ordered) {
            if (e.isSentOutbound()) {
                lastSendPerChannel.merge(e.channel().followUpChannel(), e.occurredAt(),
                        (a, b) -> b.isAfter(a) ? b : a);
            }
        }

        return lastSendPerChannel.entrySet().stream()
                .filter(entry -> lastInbound == null || lastInbound.isBefore(entry.getValue()))
                .filter(entry -> unansweredPer.getOrDefault(entry.getKey(), 0)
                        < UNANSWERED_CAP_PER_CHANNEL)
                .filter(entry -> !entry.getValue().plus(FOLLOW_UP_AFTER).isAfter(now))
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey);
    }
}
