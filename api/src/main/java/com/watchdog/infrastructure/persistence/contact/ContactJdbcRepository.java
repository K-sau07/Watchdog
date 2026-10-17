package com.watchdog.infrastructure.persistence.contact;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JDBC repository for {@link ContactRow}. Infrastructure-internal; the
 * domain talks to {@link com.watchdog.domain.port.ContactRepository} via the adapter.
 */
interface ContactJdbcRepository extends CrudRepository<ContactRow, UUID> {

    /**
     * Everyone this user has reached out to. Ordered company-first so the board's
     * grouping pass sees each company's people together, then newest reach-out first
     * within a company.
     */
    @Query("SELECT * FROM contact WHERE user_id = :userId "
            + "ORDER BY company_name ASC, reached_out_at DESC")
    List<ContactRow> findByUser(@Param("userId") UUID userId);

    /** One human, one row — the natural key behind uq_contact_user_person. */
    @Query("SELECT * FROM contact WHERE user_id = :userId "
            + "AND company_name = :companyName AND person_name = :personName")
    Optional<ContactRow> findByPerson(@Param("userId") UUID userId,
                                      @Param("companyName") String companyName,
                                      @Param("personName") String personName);
}
