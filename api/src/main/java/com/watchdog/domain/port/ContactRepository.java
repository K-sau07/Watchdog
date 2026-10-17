package com.watchdog.domain.port;

import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.Contact;

import java.util.List;
import java.util.Optional;

/** Persistence port for {@link Contact} (the reach-out layer). */
public interface ContactRepository {

    Contact save(Contact contact);

    Optional<Contact> findById(ContactId id);

    /** Everyone this user has reached out to, newest reach-out first. */
    List<Contact> findByUser(UserId userId);

    /**
     * One person at one company, if already recorded. Reaching out twice to the same
     * human must update the existing row rather than create a second one — one human,
     * one row, mirroring the one-human-one-file rule in the vault.
     */
    Optional<Contact> findByPerson(UserId userId, String companyName, String personName);
}
