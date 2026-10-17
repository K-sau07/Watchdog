package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTypeParserTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "Software Engineer",
            "Senior Backend Engineer",
            "Staff Machine Learning Engineer",
            "Tech Lead, Payments",
            "Principal Architect",
            "Data Scientist",
    })
    void individualContributorTitles(String title) {
        assertThat(RoleTypeParser.parse(title)).isEqualTo(RoleType.IC);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Engineering Manager",
            "Senior Manager, Platform",
            "Director of Engineering",
            "Head of Infrastructure",
            "VP of Engineering",
            "Chief Technology Officer",
    })
    void peopleManagerTitles(String title) {
        assertThat(RoleTypeParser.parse(title)).isEqualTo(RoleType.MANAGER);
    }

    /**
     * The case the generic "manager" rule gets wrong. A product manager has no direct
     * reports — the word names the thing they own. Filtering out management must not
     * also filter out every PM role.
     */
    @ParameterizedTest
    @ValueSource(strings = {
            "Product Manager",
            "Senior Product Manager, Growth",
            "Technical Program Manager",
            "Project Manager",
            "Account Manager",
    })
    void managerInTheTitleThatIsStillAnIcRole(String title) {
        assertThat(RoleTypeParser.parse(title)).isEqualTo(RoleType.IC);
    }

    @Test
    void unknownOnlyWhenThereIsNoTitleToRead() {
        assertThat(RoleTypeParser.parse(null)).isEqualTo(RoleType.UNKNOWN);
        assertThat(RoleTypeParser.parse("  ")).isEqualTo(RoleType.UNKNOWN);
    }

    @Test
    void isCaseInsensitive() {
        assertThat(RoleTypeParser.parse("DIRECTOR OF ENGINEERING")).isEqualTo(RoleType.MANAGER);
        assertThat(RoleTypeParser.parse("software engineer")).isEqualTo(RoleType.IC);
    }
}
