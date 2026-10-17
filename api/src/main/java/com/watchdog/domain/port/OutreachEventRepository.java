package com.watchdog.domain.port;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.OutreachEvent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Append-only persistence port for {@link OutreachEvent}. */
public interface OutreachEventRepository {

    OutreachEvent append(OutreachEvent event);

    List<OutreachEvent> findByContact(ContactId contactId);

    /**
     * Every event for every one of this user's contacts, keyed by contact.
     *
     * <p>The board projects all contacts at once, so this is one query rather than one
     * per person — with 20 companies at 8 people a week that is the difference between
     * a single round trip and a hundred and sixty.
     */
    Map<ContactId, List<OutreachEvent>> findByUserGroupedByContact(UserId userId);

    /**
     * Sent outbound events on one channel since an instant. Backs the weekly invite
     * count, which is why it counts events rather than reading a stored total — a
     * counter that is incremented by hand is a counter that drifts.
     */
    long countSentSince(UserId userId, com.watchdog.domain.model.Channel channel, Instant since);
}
