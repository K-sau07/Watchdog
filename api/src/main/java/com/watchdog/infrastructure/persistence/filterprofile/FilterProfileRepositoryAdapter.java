package com.watchdog.infrastructure.persistence.filterprofile;

import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.domain.id.UserId;
import com.watchdog.domain.model.AtsSource;
import com.watchdog.domain.model.EmploymentType;
import com.watchdog.domain.model.FilterCriteria;
import com.watchdog.domain.model.FilterProfile;
import com.watchdog.domain.model.RemoteType;
import com.watchdog.domain.model.Seniority;
import com.watchdog.domain.port.FilterProfileRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Adapter implementing {@link FilterProfileRepository} over Spring Data JDBC. Maps
 * FilterProfile (wrapping a {@link FilterCriteria}) &lt;-&gt; FilterProfileRow: the
 * §5 filter dimensions become table columns, with {@code List↔String[]}, enum-set↔
 * text[], enum↔text, and {@code Duration↔seconds} conversions confined here so the
 * domain stays pure.
 *
 * <p>Note: {@code FilterCriteria} has no {@code sources}/{@code statesToShow} columns in
 * v1 (a persisted profile constrains the posting-intrinsic + user dimensions the spec
 * lists for filter_profile §4); those live only on the live query. Persisted profiles
 * therefore round-trip with empty sources/statesToShow.
 */
@Component
class FilterProfileRepositoryAdapter implements FilterProfileRepository {

    private final FilterProfileJdbcRepository jdbc;

    FilterProfileRepositoryAdapter(FilterProfileJdbcRepository jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public FilterProfile save(FilterProfile profile) {
        boolean isNew = !jdbc.existsById(profile.id().value());
        return toDomain(jdbc.save(toRow(profile, isNew)));
    }

    @Override
    public Optional<FilterProfile> findById(FilterProfileId id) {
        return jdbc.findById(id.value()).map(FilterProfileRepositoryAdapter::toDomain);
    }

    @Override
    public List<FilterProfile> findByUser(UserId userId) {
        return jdbc.findByUserId(userId.value()).stream()
                .map(FilterProfileRepositoryAdapter::toDomain)
                .toList();
    }

    // --- mapping: domain -> row ---

    private static FilterProfileRow toRow(FilterProfile p, boolean isNew) {
        FilterCriteria c = p.criteria();
        return new FilterProfileRow(
                p.id().value(), p.userId().value(), p.name(),
                toArray(c.roleKeywords()), toArray(c.includeKeywords()), toArray(c.excludeKeywords()),
                toArray(c.locations()), enumName(c.remoteTypes()), enumArray(c.seniorities()),
                enumArray(c.employmentTypes()), c.salaryMin(),
                c.sponsorshipPref() == null ? null : c.sponsorshipPref().name(),
                c.postedWithin() == null ? null : c.postedWithin().toSeconds(),
                c.seenFrom(), c.seenTo(),
                p.createdAt(), p.updatedAt(), isNew);
    }

    // --- mapping: row -> domain ---

    private static FilterProfile toDomain(FilterProfileRow r) {
        FilterCriteria.Builder b = FilterCriteria.builder()
                .roleKeywords(toList(r.getRoleKeywords()))
                .includeKeywords(toList(r.getIncludeKeywords()))
                .excludeKeywords(toList(r.getExcludeKeywords()))
                .locations(toList(r.getLocations()))
                .seniorities(toEnumSet(r.getSeniorities(), Seniority::valueOf, Seniority.class))
                .employmentTypes(toEnumSet(r.getEmploymentTypes(), EmploymentType::valueOf, EmploymentType.class))
                .salaryMin(r.getSalaryMin())
                .seenFrom(r.getSeenFrom())
                .seenTo(r.getSeenTo());

        if (r.getRemotePref() != null) {
            b.remoteTypes(Set.of(RemoteType.valueOf(r.getRemotePref())));
        }
        if (r.getSponsorshipPref() != null) {
            b.sponsorshipPref(FilterCriteria.SponsorshipPref.valueOf(r.getSponsorshipPref()));
        }
        if (r.getPostedWithinSec() != null) {
            b.postedWithin(Duration.ofSeconds(r.getPostedWithinSec()));
        }

        return new FilterProfile(
                FilterProfileId.of(r.getId()), UserId.of(r.getUserId()), r.getName(),
                b.build(), r.getCreatedAt(), r.getUpdatedAt());
    }

    // --- conversion helpers ---

    private static String[] toArray(List<String> list) {
        return list == null ? new String[0] : list.toArray(new String[0]);
    }

    private static List<String> toList(String[] array) {
        return array == null ? List.of() : Arrays.asList(array);
    }

    private static <E extends Enum<E>> String[] enumArray(Set<E> set) {
        if (set == null || set.isEmpty()) return new String[0];
        return set.stream().map(Enum::name).toArray(String[]::new);
    }

    /** Single-enum column: take the first (a profile's remote pref is one value). */
    private static String enumName(Set<RemoteType> set) {
        if (set == null || set.isEmpty()) return null;
        return set.iterator().next().name();
    }

    private static <E extends Enum<E>> Set<E> toEnumSet(
            String[] names, Function<String, E> parse, Class<E> type) {
        if (names == null || names.length == 0) return Set.of();
        return Arrays.stream(names).map(parse).collect(Collectors.toCollection(() -> EnumSet.noneOf(type)));
    }
}
