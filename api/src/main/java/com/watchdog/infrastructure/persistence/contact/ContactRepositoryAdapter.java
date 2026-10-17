package com.watchdog.infrastructure.persistence.contact;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.CanGive;
import com.watchdog.domain.model.Contact;
import com.watchdog.domain.model.OutreachStage;
import com.watchdog.domain.model.PersonType;
import com.watchdog.domain.port.ContactRepository;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Adapter implementing {@link ContactRepository} over Spring Data JDBC. Every enum and
 * id conversion lives here so the domain record stays framework-free.
 */
@Component
class ContactRepositoryAdapter implements ContactRepository {

    private final ContactJdbcRepository jdbc;
    private final Clock clock;

    ContactRepositoryAdapter(ContactJdbcRepository jdbc, Clock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    @Override
    public Contact save(Contact contact) {
        boolean isNew = !jdbc.existsById(contact.id().value());
        return toDomain(jdbc.save(toRow(contact, isNew)));
    }

    @Override
    public Optional<Contact> findById(ContactId id) {
        return jdbc.findById(id.value()).map(ContactRepositoryAdapter::toDomain);
    }

    @Override
    public List<Contact> findByUser(UserId userId) {
        return jdbc.findByUser(userId.value()).stream()
                .map(ContactRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public Optional<Contact> findByPerson(UserId userId, String companyName, String personName) {
        return jdbc.findByPerson(userId.value(), companyName, personName)
                .map(ContactRepositoryAdapter::toDomain);
    }

    // --- mapping ---

    private ContactRow toRow(Contact c, boolean isNew) {
        return new ContactRow(
                c.id().value(),
                c.userId().value(),
                c.companyName(),
                c.postingId() == null ? null : c.postingId().value(),
                c.personName(),
                c.personTitle(),
                c.linkedinUrl(),
                c.email(),
                c.personType() == null ? null : c.personType().name(),
                c.canGive() == null ? null : c.canGive().name(),
                c.stageOverride() == null ? null : c.stageOverride().name(),
                c.reachedOutAt(),
                // Kept as a denormalised convenience for ordering; the authoritative
                // timeline is outreach_event.
                c.reachedOutAt(),
                Instant.now(clock),
                isNew);
    }

    private static Contact toDomain(ContactRow r) {
        return new Contact(
                ContactId.of(r.getId()),
                UserId.of(r.getUserId()),
                r.getCompanyName(),
                r.getPostingId() == null ? null : PostingId.of(r.getPostingId()),
                r.getPersonName(),
                r.getPersonTitle(),
                r.getLinkedinUrl(),
                r.getEmail(),
                r.getPersonType() == null ? null : PersonType.valueOf(r.getPersonType()),
                r.getCanGive() == null ? null : CanGive.valueOf(r.getCanGive()),
                r.getStageOverride() == null ? null : OutreachStage.valueOf(r.getStageOverride()),
                r.getReachedOutAt());
    }
}
