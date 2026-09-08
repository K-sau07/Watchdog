package com.watchdog.infrastructure.persistence.filterprofile;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Spring Data JDBC repository for {@link FilterProfileRow}. Infrastructure-internal. */
interface FilterProfileJdbcRepository extends CrudRepository<FilterProfileRow, UUID> {

    @Query("SELECT * FROM filter_profile WHERE user_id = :userId ORDER BY updated_at DESC")
    List<FilterProfileRow> findByUserId(@Param("userId") UUID userId);
}
