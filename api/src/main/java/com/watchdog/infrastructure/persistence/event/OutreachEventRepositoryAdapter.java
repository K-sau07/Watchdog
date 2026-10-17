package com.watchdog.infrastructure.persistence.event;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.OutreachEventId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.Channel;
import com.watchdog.domain.model.EventKind;
import com.watchdog.domain.model.OutreachEvent;
import com.watchdog.domain.port.OutreachEventRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Adapter implementing {@link OutreachEventRepository} over Spring Data JDBC. */
@Component
class OutreachEventRepositoryAdapter implements OutreachEventRepository {

    private final OutreachEventJdbcRepository jdbc;

    OutreachEventRepositoryAdapter(OutreachEventJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OutreachEvent append(OutreachEvent event) {
        return toDomain(jdbc.save(toRow(event)));
    }

    @Override
    public List<OutreachEvent> findByContact(ContactId contactId) {
        return jdbc.findByContact(contactId.value()).stream()
                .map(OutreachEventRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Map<ContactId, List<OutreachEvent>> findByUserGroupedByContact(UserId userId) {
        return jdbc.findByUser(userId.value()).stream()
                .map(OutreachEventRepositoryAdapter::toDomain)
                .collect(Collectors.groupingBy(OutreachEvent::contactId));
    }

    @Override
    public long countSentSince(UserId userId, Channel channel, Instant since) {
        return jdbc.countSentSince(userId.value(), channel.name(), since);
    }

    // --- mapping ---

    private static OutreachEventRow toRow(OutreachEvent e) {
        return new OutreachEventRow(
                e.id().value(),
                e.contactId().value(),
                e.direction().name(),
                e.channel().name(),
                e.kind().name(),
                e.status().name(),
                e.body(),
                e.occurredAt(),
                true);
    }

    private static OutreachEvent toDomain(OutreachEventRow r) {
        return new OutreachEvent(
                OutreachEventId.of(r.getId()),
                ContactId.of(r.getContactId()),
                OutreachEvent.Direction.valueOf(r.getDirection()),
                Channel.valueOf(r.getChannel()),
                EventKind.valueOf(r.getKind()),
                OutreachEvent.Status.valueOf(r.getStatus()),
                r.getBody(),
                r.getOccurredAt());
    }
}
