package com.watchdog.infrastructure.web;

import com.watchdog.application.outreach.OutreachProjection;
import com.watchdog.application.outreach.OutreachService;
import com.watchdog.application.outreach.OutreachService.CompanyGroup;
import com.watchdog.application.outreach.OutreachService.ContactNotFound;
import com.watchdog.application.outreach.OutreachService.Pace;
import com.watchdog.application.outreach.OutreachService.Person;
import com.watchdog.domain.id.ContactId;
import com.watchdog.domain.id.PostingId;
import com.watchdog.domain.model.CanGive;
import com.watchdog.domain.model.Channel;
import com.watchdog.domain.model.EventKind;
import com.watchdog.domain.model.OutreachEvent;
import com.watchdog.domain.model.OutreachStage;
import com.watchdog.domain.model.PersonType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Outreach endpoints: the Started board, the day's follow-ups, this week's pace, and the
 * writes behind them. Thin over {@link OutreachService}.
 */
@RestController
@RequestMapping("/api/outreach")
class OutreachController {

    private final OutreachService outreach;

    OutreachController(OutreachService outreach) {
        this.outreach = outreach;
    }

    record PersonResponse(
            String id,
            String companyName,
            String personName,
            String personTitle,
            String linkedinUrl,
            String email,
            String personType,
            String canGive,
            String stage,
            boolean stageDeclared,
            int turns,
            int unanswered,
            List<String> channelsTouched,
            Map<String, Integer> unansweredPerChannel,
            Instant reachedOutAt,
            Instant lastOutboundAt,
            Instant lastInboundAt,
            /** Null unless a chase is owed; otherwise the channel to send it on. */
            String followUpDueOn,
            String postingId
    ) {
        static PersonResponse from(Person p) {
            OutreachProjection.State s = p.state();
            return new PersonResponse(
                    p.contact().id().toString(),
                    p.contact().companyName(),
                    p.contact().personName(),
                    p.contact().personTitle(),
                    p.contact().linkedinUrl(),
                    p.contact().email(),
                    p.contact().personType() == null ? null : p.contact().personType().name(),
                    p.contact().canGive() == null ? null : p.contact().canGive().name(),
                    s.stage().name(),
                    p.contact().stageOverride() != null,
                    s.turns(),
                    s.unanswered(),
                    s.channelsTouched().stream().map(Enum::name).sorted().toList(),
                    s.unansweredPerChannel().entrySet().stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    e -> e.getKey().name(), Map.Entry::getValue)),
                    p.contact().reachedOutAt(),
                    s.lastOutboundAt(),
                    s.lastInboundAt(),
                    s.followUpDueOn().map(Enum::name).orElse(null),
                    p.contact().postingId() == null ? null : p.contact().postingId().toString());
        }
    }

    record CompanyResponse(
            String companyName,
            String fundingStage,
            int target,
            int invited,
            int emailed,
            List<PersonResponse> awaitingReply,
            List<PersonResponse> inConversation
    ) {
        static CompanyResponse from(CompanyGroup g) {
            // The split the board is built around: people who have not written back and
            // need chasing, and people mid-conversation who need answering. Done here
            // rather than in the client so both stay one definition.
            List<PersonResponse> awaiting = g.people().stream()
                    .filter(p -> !p.state().hasReplied())
                    .sorted(Comparator.comparing(
                            (Person p) -> p.state().followUpDueOn().isPresent() ? 0 : 1))
                    .map(PersonResponse::from)
                    .toList();
            List<PersonResponse> talking = g.people().stream()
                    .filter(p -> p.state().hasReplied())
                    .map(PersonResponse::from)
                    .toList();
            return new CompanyResponse(
                    g.companyName(),
                    g.fundingStage() == null ? null : g.fundingStage().name(),
                    g.target(), g.invited(), g.emailed(), awaiting, talking);
        }
    }

    @GetMapping
    public List<CompanyResponse> board() {
        return outreach.board().stream().map(CompanyResponse::from).toList();
    }

    @GetMapping("/follow-ups")
    public List<PersonResponse> followUps() {
        return outreach.followUpsDue().stream().map(PersonResponse::from).toList();
    }

    record PaceResponse(int invitesSent, int inviteCap, long companiesTouched,
                        int companiesTarget, boolean invitesBurned) {
        static PaceResponse from(Pace p) {
            return new PaceResponse(p.invitesSent(), p.inviteCap(), p.companiesTouched(),
                    p.companiesTarget(), p.invitesBurned());
        }
    }

    @GetMapping("/pace")
    public PaceResponse pace() {
        return PaceResponse.from(outreach.pace());
    }

    /** Put someone on a roster and record the first touch. */
    record TargetRequest(
            String companyName,
            String personName,
            String personTitle,
            String linkedinUrl,
            String email,
            String personType,
            String postingId,
            String channel,
            String kind,
            String status,
            String body
    ) {}

    @PostMapping
    public ResponseEntity<PersonResponse> target(@RequestBody TargetRequest body) {
        PostingId posting = blank(body.postingId()) ? null : PostingId.of(body.postingId());
        Person saved = outreach.target(
                body.companyName(), body.personName(), body.personTitle(), body.linkedinUrl(),
                body.email(),
                blank(body.personType()) ? null : PersonType.valueOf(body.personType()),
                posting,
                blank(body.channel()) ? Channel.LINKEDIN_INVITE : Channel.valueOf(body.channel()),
                blank(body.kind()) ? EventKind.INVITE : EventKind.valueOf(body.kind()),
                body.body(),
                blank(body.status()) ? OutreachEvent.Status.SENT
                        : OutreachEvent.Status.valueOf(body.status()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PersonResponse.from(saved));
    }

    /** Record a touch or a reply against someone already on a roster. */
    record EventRequest(String direction, String channel, String kind, String status, String body) {}

    @PostMapping("/{id}/events")
    public PersonResponse recordEvent(@PathVariable String id, @RequestBody EventRequest body) {
        OutreachEvent.Direction direction = blank(body.direction())
                ? OutreachEvent.Direction.OUTBOUND
                : OutreachEvent.Direction.valueOf(body.direction());
        EventKind kind = blank(body.kind())
                ? (direction == OutreachEvent.Direction.INBOUND ? EventKind.REPLY : EventKind.FOLLOW_UP)
                : EventKind.valueOf(body.kind());

        return PersonResponse.from(outreach.recordEvent(
                ContactId.of(id),
                direction,
                blank(body.channel()) ? Channel.LINKEDIN_DM : Channel.valueOf(body.channel()),
                kind,
                blank(body.status()) ? OutreachEvent.Status.SENT
                        : OutreachEvent.Status.valueOf(body.status()),
                body.body()));
    }

    @PatchMapping("/{id}")
    public PersonResponse patch(@PathVariable String id, @RequestBody Map<String, Object> body) {
        String stage = (String) body.get("stageOverride");
        boolean clearStage = body.containsKey("stageOverride") && stage == null;

        return PersonResponse.from(outreach.patch(
                ContactId.of(id),
                stage == null ? null : OutreachStage.valueOf(stage),
                body.get("canGive") == null ? null : CanGive.valueOf((String) body.get("canGive")),
                (String) body.get("email"),
                body.get("personType") == null
                        ? null : PersonType.valueOf((String) body.get("personType")),
                clearStage));
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    @ExceptionHandler(ContactNotFound.class)
    ResponseEntity<Map<String, String>> notFound(ContactNotFound e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
    }

    /** Bad enum names and blank required fields are 400, never a silent no-op (§10). */
    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
    ResponseEntity<Map<String, String>> badRequest(RuntimeException e) {
        return ResponseEntity.badRequest().body(Map.of("error", String.valueOf(e.getMessage())));
    }
}
