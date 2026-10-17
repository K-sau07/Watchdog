package com.watchdog.domain.model;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.OutreachEventId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * One thing that happened in a conversation: a message sent, or a reply received.
 *
 * <p>Append-only. Events are the source of truth for a contact's stage and counters —
 * see {@link com.watchdog.domain.model.OutreachEvent} consumers in the application layer.
 * Nothing here is ever edited; a correction is a new event.
 */
public record OutreachEvent(
        OutreachEventId id,
        ContactId contactId,
        Direction direction,
        Channel channel,
        EventKind kind,
        Status status,
        String body,
        Instant occurredAt
) {

    public enum Direction { OUTBOUND, INBOUND }

    /**
     * {@link #DRAFTED} is written but not sent. Only {@link #SENT} counts against a cap
     * or starts a follow-up clock — a draft sitting in the app has reached nobody.
     */
    public enum Status { DRAFTED, SENT }

    public OutreachEvent {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(contactId, "contactId must not be null");
        Objects.requireNonNull(direction, "direction must not be null");
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        status = status == null ? Status.SENT : status;

        if (direction == Direction.INBOUND && kind != EventKind.REPLY) {
            throw new IllegalArgumentException("An inbound event is a REPLY; got " + kind);
        }
        if (direction == Direction.INBOUND && status == Status.DRAFTED) {
            throw new IllegalArgumentException("A received reply cannot be a draft");
        }
    }

    /** A message that actually went out. */
    public static OutreachEvent sent(ContactId contactId, Channel channel, EventKind kind,
                                     String body, Instant when) {
        return new OutreachEvent(OutreachEventId.generate(), contactId, Direction.OUTBOUND,
                channel, kind, Status.SENT, body, when);
    }

    /** Written, not yet sent. Starts no clock and spends no budget. */
    public static OutreachEvent drafted(ContactId contactId, Channel channel, EventKind kind,
                                        String body, Instant when) {
        return new OutreachEvent(OutreachEventId.generate(), contactId, Direction.OUTBOUND,
                channel, kind, Status.DRAFTED, body, when);
    }

    /** They wrote something real. */
    public static OutreachEvent reply(ContactId contactId, Channel channel, String body,
                                      Instant when) {
        return new OutreachEvent(OutreachEventId.generate(), contactId, Direction.INBOUND,
                channel, EventKind.REPLY, Status.SENT, body, when);
    }

    public Optional<String> bodyOpt() {
        return Optional.ofNullable(body);
    }

    /** True when this is an outbound message that actually reached someone. */
    public boolean isSentOutbound() {
        return direction == Direction.OUTBOUND && status == Status.SENT;
    }

    /** True when this send counts against the unanswered cap — i.e. not an invite. */
    public boolean countsAsUnanswered() {
        return isSentOutbound() && channel.countsAsMessage();
    }
}
