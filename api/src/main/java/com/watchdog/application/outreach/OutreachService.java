package com.watchdog.application.outreach;

import com.watchdog.application.matching.SingleUser;
import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.CanGive;
import com.watchdog.domain.model.Channel;
import com.watchdog.domain.model.Company;
import com.watchdog.domain.model.Contact;
import com.watchdog.domain.model.EventKind;
import com.watchdog.domain.model.FundingStage;
import com.watchdog.domain.model.OutreachEvent;
import com.watchdog.domain.model.OutreachStage;
import com.watchdog.domain.model.PersonType;
import com.watchdog.domain.port.CompanyRepository;
import com.watchdog.domain.port.ContactRepository;
import com.watchdog.domain.port.OutreachEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * The outreach board: who is on each company's roster, what has been said to them, and
 * what is owed today.
 *
 * <p>Grouping is by company because that is how the hunt works — a posting puts a
 * company on the radar and then a roster of people inside it get approached about it on
 * both channels.
 */
@Service
public class OutreachService {

    private final ContactRepository contacts;
    private final OutreachEventRepository events;
    private final CompanyRepository companies;
    private final Clock clock;
    private final int targetPerCompany;
    private final int companiesPerWeek;
    private final int inviteCapPerWeek;

    public OutreachService(ContactRepository contacts, OutreachEventRepository events,
                           CompanyRepository companies, Clock clock,
                           @Value("${watchdog.outreach.target-per-company:8}") int targetPerCompany,
                           @Value("${watchdog.outreach.companies-per-week:20}") int companiesPerWeek,
                           @Value("${watchdog.outreach.invite-cap-per-week:200}") int inviteCapPerWeek) {
        this.contacts = contacts;
        this.events = events;
        this.companies = companies;
        this.clock = clock;
        this.targetPerCompany = targetPerCompany;
        this.companiesPerWeek = companiesPerWeek;
        this.inviteCapPerWeek = inviteCapPerWeek;
    }

    /** Thrown when a request names a contact that does not exist. */
    public static class ContactNotFound extends RuntimeException {
        public ContactNotFound(ContactId id) {
            super("No contact " + id);
        }
    }

    /** One person plus everything derived from their event log. */
    public record Person(Contact contact, OutreachProjection.State state) {}

    /** One company tag, its roster, and how much of the target it covers. */
    public record CompanyGroup(
            String companyName,
            FundingStage fundingStage,
            List<Person> people,
            int target,
            int invited,
            int emailed
    ) {}

    /** How much of this week's budget is spent. */
    public record Pace(
            int invitesSent,
            int inviteCap,
            long companiesTouched,
            int companiesTarget
    ) {
        public boolean invitesBurned() {
            return invitesSent >= inviteCap;
        }
    }

    @Transactional(readOnly = true)
    public List<CompanyGroup> board() {
        Instant now = Instant.now(clock);
        List<Contact> all = contacts.findByUser(SingleUser.ID);
        Map<ContactId, List<OutreachEvent>> byContact =
                events.findByUserGroupedByContact(SingleUser.ID);

        Map<String, List<Person>> byCompany = new LinkedHashMap<>();
        for (Contact c : all) {
            OutreachProjection.State state = OutreachProjection.of(
                    byContact.getOrDefault(c.id(), List.of()), c.stageOverride(), now);
            byCompany.computeIfAbsent(c.companyName(), k -> new ArrayList<>())
                    .add(new Person(c, state));
        }

        // One pass over the registry rather than a lookup per group. Names are matched
        // case-insensitively: a contact's company is free text typed by a human and the
        // registry's is whatever the ATS called it.
        Map<String, FundingStage> stages = new HashMap<>();
        for (Company c : companies.findActive()) {
            stages.putIfAbsent(c.name().toLowerCase(Locale.ROOT), c.fundingStage());
        }

        return byCompany.entrySet().stream()
                .map(e -> {
                    List<Person> people = List.copyOf(e.getValue());
                    return new CompanyGroup(
                            e.getKey(),
                            stages.getOrDefault(e.getKey().toLowerCase(Locale.ROOT),
                                    FundingStage.UNKNOWN),
                            people,
                            targetPerCompany,
                            (int) people.stream()
                                    .filter(p -> p.state().channelsTouched().stream()
                                            .anyMatch(ch -> ch != Channel.EMAIL))
                                    .count(),
                            (int) people.stream()
                                    .filter(p -> p.state().channelsTouched().contains(Channel.EMAIL))
                                    .count());
                })
                .toList();
    }

    /**
     * Everyone owed a chase right now, coldest first.
     *
     * <p>This is the day's work, so it is a query of its own rather than something the
     * client filters out of the board — a follow-up that needs scrolling to find is a
     * follow-up that does not get sent.
     */
    @Transactional(readOnly = true)
    public List<Person> followUpsDue() {
        return board().stream()
                .flatMap(g -> g.people().stream())
                .filter(p -> p.state().followUpDueOn().isPresent())
                .sorted((a, b) -> {
                    Instant ta = a.state().lastOutboundAt();
                    Instant tb = b.state().lastOutboundAt();
                    if (ta == null || tb == null) {
                        return 0;
                    }
                    return ta.compareTo(tb);
                })
                .toList();
    }

    /**
     * This week's spend. The invite count is a query over the event log rather than a
     * stored total, because a counter incremented by hand is a counter that drifts.
     */
    @Transactional(readOnly = true)
    public Pace pace() {
        Instant weekStart = startOfWeek();
        long invites = events.countSentSince(SingleUser.ID, Channel.LINKEDIN_INVITE, weekStart);

        long companiesTouched = contacts.findByUser(SingleUser.ID).stream()
                .filter(c -> !c.reachedOutAt().isBefore(weekStart))
                .map(c -> c.companyName().toLowerCase(Locale.ROOT))
                .distinct()
                .count();

        return new Pace((int) invites, inviteCapPerWeek, companiesTouched, companiesPerWeek);
    }

    /** Monday 00:00 UTC. The invite allowance is weekly, so the window must be too. */
    private Instant startOfWeek() {
        return Instant.now(clock)
                .atZone(ZoneOffset.UTC)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                .toLocalDate()
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
    }

    /**
     * Put someone on a company's roster and record the first touch.
     *
     * <p>Idempotent on the natural key: adding the same human twice updates their details
     * and appends another event rather than creating a second row. One human, one row,
     * mirroring the one-human-one-file rule in the vault.
     */
    @Transactional
    public Person target(String companyName, String personName, String personTitle,
                         String linkedinUrl, String email, PersonType personType,
                         PostingId postingId, Channel channel, EventKind kind, String body,
                         OutreachEvent.Status status) {
        Instant now = Instant.now(clock);

        Contact contact = contacts.findByPerson(SingleUser.ID, companyName, personName)
                .map(existing -> {
                    Contact updated = existing;
                    if (email != null && !email.isBlank()) {
                        updated = updated.withEmail(email);
                    }
                    if (personType != null) {
                        updated = updated.withPersonType(personType);
                    }
                    return updated == existing ? existing : contacts.save(updated);
                })
                .orElseGet(() -> contacts.save(Contact.target(SingleUser.ID, companyName,
                        personName, personTitle, linkedinUrl, email, personType, postingId, now)));

        events.append(new OutreachEvent(
                com.watchdog.domain.id.OutreachEventId.generate(),
                contact.id(), OutreachEvent.Direction.OUTBOUND, channel, kind,
                status == null ? OutreachEvent.Status.SENT : status, body, now));

        return project(contact, now);
    }

    /** Record a touch or a reply against someone already on a roster. */
    @Transactional
    public Person recordEvent(ContactId id, OutreachEvent.Direction direction, Channel channel,
                              EventKind kind, OutreachEvent.Status status, String body) {
        Contact contact = contacts.findById(id).orElseThrow(() -> new ContactNotFound(id));
        Instant now = Instant.now(clock);

        events.append(new OutreachEvent(
                com.watchdog.domain.id.OutreachEventId.generate(),
                contact.id(), direction, channel, kind,
                status == null ? OutreachEvent.Status.SENT : status, body, now));

        return project(contact, now);
    }

    /**
     * Edit the things that are not events: the address, the routing, a declared stage.
     * Absent fields are left alone.
     */
    @Transactional
    public Person patch(ContactId id, OutreachStage stageOverride, CanGive canGive,
                        String email, PersonType personType, boolean clearStage) {
        Contact c = contacts.findById(id).orElseThrow(() -> new ContactNotFound(id));
        if (stageOverride != null || clearStage) {
            c = c.withStageOverride(stageOverride);
        }
        if (canGive != null) {
            c = c.withCanGive(canGive);
        }
        if (email != null) {
            c = c.withEmail(email);
        }
        if (personType != null) {
            c = c.withPersonType(personType);
        }
        return project(contacts.save(c), Instant.now(clock));
    }

    private Person project(Contact contact, Instant now) {
        return new Person(contact, OutreachProjection.of(
                events.findByContact(contact.id()), contact.stageOverride(), now));
    }

    /** Per-channel unanswered budget, for callers that want to show what is left. */
    public Map<Channel, Integer> remainingBudget(OutreachProjection.State state) {
        Map<Channel, Integer> left = new EnumMap<>(Channel.class);
        state.unansweredPerChannel().forEach((ch, used) ->
                left.put(ch, Math.max(0, OutreachProjection.UNANSWERED_CAP_PER_CHANNEL - used)));
        return left;
    }

    public Optional<Integer> targetPerCompany() {
        return Optional.of(targetPerCompany);
    }
}
