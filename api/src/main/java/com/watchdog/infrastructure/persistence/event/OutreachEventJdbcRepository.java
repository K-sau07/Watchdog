package com.watchdog.infrastructure.persistence.event;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spring Data JDBC repository for {@link OutreachEventRow}. Infrastructure-internal. */
interface OutreachEventJdbcRepository extends CrudRepository<OutreachEventRow, UUID> {

    @Query("SELECT * FROM outreach_event WHERE contact_id = :contactId ORDER BY occurred_at ASC")
    List<OutreachEventRow> findByContact(@Param("contactId") UUID contactId);

    /** Every event belonging to this user's contacts, in one pass. */
    @Query("SELECT e.* FROM outreach_event e JOIN contact c ON c.id = e.contact_id "
            + "WHERE c.user_id = :userId ORDER BY e.occurred_at ASC")
    List<OutreachEventRow> findByUser(@Param("userId") UUID userId);

    @Query("SELECT count(*) FROM outreach_event e JOIN contact c ON c.id = e.contact_id "
            + "WHERE c.user_id = :userId AND e.channel = :channel "
            + "AND e.direction = 'OUTBOUND' AND e.status = 'SENT' AND e.occurred_at >= :since")
    long countSentSince(@Param("userId") UUID userId,
                        @Param("channel") String channel,
                        @Param("since") Instant since);
}
