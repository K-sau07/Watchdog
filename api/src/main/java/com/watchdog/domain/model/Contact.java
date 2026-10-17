package com.watchdog.domain.model;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.id.UserId;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A person being approached at a company. Who they are and how to reach them — nothing
 * about what has been said to them.
 *
 * <p>Stage, turn count and unanswered counters used to live here as stored fields with
 * the cap rules as methods. They moved to {@link
 * com.watchdog.application.outreach.OutreachProjection}, computed from the event log,
 * once a second channel arrived: two places maintaining the same counter is the failure
 * Pipeline/core/STAGES.md was written to escape, and having the rules in a record *and*
 * a projection would have been exactly that.
 *
 * <p>What survives here is a declared stage — ASKED, WON, DEAD and NURTURE cannot be
 * derived from events, because no event says "they sounded irritated".
 *
 * <p>{@code companyName} is free text, not a {@link com.watchdog.domain.id.CompanyId}:
 * people get approached at companies whose boards we do not watch, and a reach-out must
 * not be unrecordable because the registry has never heard of the place.
 */
public record Contact(
        ContactId id,
        UserId userId,
        String companyName,
        PostingId postingId,
        String personName,
        String personTitle,
        String linkedinUrl,
        String email,
        PersonType personType,
        CanGive canGive,
        /** A stage a human stated. Null means derive it from the events. */
        OutreachStage stageOverride,
        Instant reachedOutAt
) {

    public Contact {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(reachedOutAt, "reachedOutAt must not be null");
        requireText(companyName, "companyName");
        requireText(personName, "personName");
        if (stageOverride != null && !isDeclarable(stageOverride)) {
            throw new IllegalArgumentException(
                    stageOverride + " is derived from events and cannot be declared");
        }
    }

    /**
     * Stages a human states rather than earns. REACHED_OUT, LIVE and EXCHANGE all follow
     * from what happened, so declaring one would let the board disagree with its own log.
     */
    public static boolean isDeclarable(OutreachStage stage) {
        return stage == OutreachStage.ASKED || stage == OutreachStage.WON
                || stage == OutreachStage.DEAD || stage == OutreachStage.NURTURE;
    }

    /** A person added to a company's roster. The approach itself is an event. */
    public static Contact target(UserId userId, String companyName, String personName,
                                 String personTitle, String linkedinUrl, String email,
                                 PersonType personType, PostingId postingId, Instant when) {
        return new Contact(ContactId.generate(), userId, companyName, postingId, personName,
                personTitle, linkedinUrl, email, personType, null, null, when);
    }

    public Optional<PostingId> postingIdOpt() {
        return Optional.ofNullable(postingId);
    }

    public Optional<String> emailOpt() {
        return Optional.ofNullable(email);
    }

    public Optional<PersonType> personTypeOpt() {
        return Optional.ofNullable(personType);
    }

    public Optional<CanGive> canGiveOpt() {
        return Optional.ofNullable(canGive);
    }

    public Optional<OutreachStage> stageOverrideOpt() {
        return Optional.ofNullable(stageOverride);
    }

    /** True once an address is on file — i.e. the email channel is usable at all. */
    public boolean isEmailable() {
        return email != null && !email.isBlank();
    }

    public Contact withStageOverride(OutreachStage next) {
        return new Contact(id, userId, companyName, postingId, personName, personTitle,
                linkedinUrl, email, personType, canGive, next, reachedOutAt);
    }

    public Contact withCanGive(CanGive next) {
        return new Contact(id, userId, companyName, postingId, personName, personTitle,
                linkedinUrl, email, personType, next, stageOverride, reachedOutAt);
    }

    public Contact withEmail(String next) {
        return new Contact(id, userId, companyName, postingId, personName, personTitle,
                linkedinUrl, next, personType, canGive, stageOverride, reachedOutAt);
    }

    public Contact withPersonType(PersonType next) {
        return new Contact(id, userId, companyName, postingId, personName, personTitle,
                linkedinUrl, email, next, canGive, stageOverride, reachedOutAt);
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
