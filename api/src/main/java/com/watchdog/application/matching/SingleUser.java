package com.watchdog.application.matching;

import com.watchdog.domain.id.UserId;

import java.util.UUID;

/**
 * The v1 single-user identity (D-WD5: auth deferred, single-user, schema stays
 * multi-user-ready). Until real auth arrives (Phase 2), every per-user operation —
 * job-state filtering here in S6, the save/applied/hide workflow in S7 — resolves to
 * this fixed id. The {@code user_id} columns already carry it, so auth slots in later
 * without a schema change.
 *
 * <p>Runtime note: the write side (S7) requires a matching {@code app_user} row seeded
 * with this id; the read side (S6) tolerates its absence — no job-state row means the
 * posting is implicitly NEW.
 */
public final class SingleUser {

    /** Stable well-known id for the sole v1 user. */
    public static final UserId ID = UserId.of(UUID.fromString("00000000-0000-0000-0000-000000000001"));

    private SingleUser() {
    }
}
