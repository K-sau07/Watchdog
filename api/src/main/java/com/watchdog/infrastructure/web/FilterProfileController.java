package com.watchdog.infrastructure.web;

import com.watchdog.application.matching.FilterProfileService;
import com.watchdog.domain.id.FilterProfileId;
import com.watchdog.infrastructure.web.FilterProfileDtos.ProfileResponse;
import com.watchdog.infrastructure.web.FilterProfileDtos.ProfileUpdate;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/**
 * Saved-filter CRUD (spec §7), single-user in v1 (D-WD5). Thin over
 * {@link FilterProfileService}.
 *
 * <ul>
 *   <li>{@code GET /api/filter-profiles/default} — the single user's profile,
 *       auto-provisioned on first access.</li>
 *   <li>{@code GET /api/filter-profiles/{id}} — a specific profile (404 if unknown).</li>
 *   <li>{@code PUT /api/filter-profiles/{id}} — update name + criteria.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/filter-profiles")
class FilterProfileController {

    private final FilterProfileService profiles;

    FilterProfileController(FilterProfileService profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/default")
    public ProfileResponse getDefault() {
        return ProfileResponse.from(profiles.getOrCreateDefault());
    }

    @GetMapping("/{id}")
    public ProfileResponse getById(@PathVariable String id) {
        return ProfileResponse.from(profiles.getById(parseId(id)));
    }

    @PutMapping("/{id}")
    public ProfileResponse update(@PathVariable String id, @RequestBody ProfileUpdate body) {
        if (body == null || body.name() == null || body.name().isBlank()) {
            throw new PostingQueryMapper.BadFilterParam("name is required");
        }
        return ProfileResponse.from(
                profiles.update(parseId(id), body.name(), body.toCriteria()));
    }

    private static FilterProfileId parseId(String raw) {
        try {
            return FilterProfileId.of(UUID.fromString(raw));
        } catch (IllegalArgumentException e) {
            throw new PostingQueryMapper.BadFilterParam("invalid profile id: '" + raw + "'");
        }
    }

    @ExceptionHandler(PostingQueryMapper.BadFilterParam.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> onBadParam(PostingQueryMapper.BadFilterParam e) {
        return Map.of("error", e.getMessage());
    }

    @ExceptionHandler(FilterProfileService.ProfileNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> onNotFound(FilterProfileService.ProfileNotFound e) {
        return Map.of("error", e.getMessage());
    }
}
