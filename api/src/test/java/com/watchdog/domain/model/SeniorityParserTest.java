package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeniorityParserTest {

    @Test
    void seniorTitles() {
        assertThat(SeniorityParser.parse("Senior Software Engineer")).isEqualTo(Seniority.SENIOR);
        assertThat(SeniorityParser.parse("Sr. Backend Engineer")).isEqualTo(Seniority.SENIOR);
        assertThat(SeniorityParser.parse("Engineering Lead")).isEqualTo(Seniority.SENIOR);
        assertThat(SeniorityParser.parse("Software Architect")).isEqualTo(Seniority.SENIOR);
    }

    @Test
    void staffAndPrincipalRollUpToSenior() {
        // The enum has no STAFF value; the very-senior tail buckets into SENIOR (see javadoc).
        assertThat(SeniorityParser.parse("Staff Software Engineer")).isEqualTo(Seniority.SENIOR);
        assertThat(SeniorityParser.parse("Principal Engineer")).isEqualTo(Seniority.SENIOR);
        assertThat(SeniorityParser.parse("Distinguished Engineer")).isEqualTo(Seniority.SENIOR);
    }

    @Test
    void midTitles() {
        assertThat(SeniorityParser.parse("Software Engineer II")).isEqualTo(Seniority.MID);
        assertThat(SeniorityParser.parse("Software Engineer III")).isEqualTo(Seniority.MID);
        assertThat(SeniorityParser.parse("Intermediate Developer")).isEqualTo(Seniority.MID);
        assertThat(SeniorityParser.parse("Mid-Level Engineer")).isEqualTo(Seniority.MID);
        assertThat(SeniorityParser.parse("Backend Engineer, 3+ years")).isEqualTo(Seniority.MID);
    }

    @Test
    void internTitles() {
        assertThat(SeniorityParser.parse("Software Engineering Intern")).isEqualTo(Seniority.INTERN);
        assertThat(SeniorityParser.parse("Engineering Internship - Summer 2026")).isEqualTo(Seniority.INTERN);
        assertThat(SeniorityParser.parse("Co-op Software Developer")).isEqualTo(Seniority.INTERN);
    }

    @Test
    void newGradTitles() {
        assertThat(SeniorityParser.parse("Software Engineer, New Grad")).isEqualTo(Seniority.NEW_GRAD);
        assertThat(SeniorityParser.parse("New Graduate Software Engineer")).isEqualTo(Seniority.NEW_GRAD);
        assertThat(SeniorityParser.parse("University Grad - Backend")).isEqualTo(Seniority.NEW_GRAD);
        assertThat(SeniorityParser.parse("Early Talent Software Engineer")).isEqualTo(Seniority.NEW_GRAD);
    }

    @Test
    void juniorTitles() {
        assertThat(SeniorityParser.parse("Junior Software Engineer")).isEqualTo(Seniority.JUNIOR);
        assertThat(SeniorityParser.parse("Jr. Developer")).isEqualTo(Seniority.JUNIOR);
        assertThat(SeniorityParser.parse("Associate Software Engineer")).isEqualTo(Seniority.JUNIOR);
        assertThat(SeniorityParser.parse("Entry-Level Engineer")).isEqualTo(Seniority.JUNIOR);
        assertThat(SeniorityParser.parse("Software Engineer I")).isEqualTo(Seniority.JUNIOR);
    }

    // --- precedence: the whole point of the ordering ---

    @Test
    void seniorWinsOverNewGradInSameTitle() {
        // "Senior ... New Grad Program" must not read as NEW_GRAD.
        assertThat(SeniorityParser.parse("Senior Software Engineer, New Grad Program"))
                .isEqualTo(Seniority.SENIOR);
    }

    @Test
    void internWinsOverNewGrad() {
        assertThat(SeniorityParser.parse("New Grad Software Engineering Intern"))
                .isEqualTo(Seniority.INTERN);
    }

    @Test
    void newGradWinsOverJunior() {
        // Mislabelling a new-grad role as JUNIOR would hide it from the default hunt.
        assertThat(SeniorityParser.parse("Associate Software Engineer, New Grad"))
                .isEqualTo(Seniority.NEW_GRAD);
    }

    @Test
    void midLevelMarkerWinsOverJuniorOneMarker() {
        // "II" (MID) is checked before the trailing "I" (JUNIOR).
        assertThat(SeniorityParser.parse("Software Engineer II")).isEqualTo(Seniority.MID);
    }

    // --- false-positive guards (word boundaries) ---

    @Test
    void doesNotMatchSubstrings() {
        // "seniority" contains "senior", "internal" contains "intern" — neither should fire.
        assertThat(SeniorityParser.parse("Engineer with seniority in mind")).isEqualTo(Seniority.UNKNOWN);
        assertThat(SeniorityParser.parse("Internal Tools Engineer")).isEqualTo(Seniority.UNKNOWN);
    }

    // --- body fallback ---

    @Test
    void bodyFallbackDetectsNewGradWhenTitleSilent() {
        Seniority s = SeniorityParser.parse(
                "Software Engineer", "We welcome new graduates; 0-1 years experience.");
        assertThat(s).isEqualTo(Seniority.NEW_GRAD);
    }

    @Test
    void bodyFallbackOnlyAppliesWhenTitleGivesNoSignal() {
        // Title says senior; a new-grad phrase in the body must not override it.
        Seniority s = SeniorityParser.parse(
                "Senior Software Engineer", "Great role for recent graduates too.");
        assertThat(s).isEqualTo(Seniority.SENIOR);
    }

    // --- honest unknown ---

    @Test
    void unknownWhenNoSignal() {
        assertThat(SeniorityParser.parse("Software Engineer")).isEqualTo(Seniority.UNKNOWN);
        assertThat(SeniorityParser.parse("Software Engineer", null)).isEqualTo(Seniority.UNKNOWN);
        assertThat(SeniorityParser.parse(null, null)).isEqualTo(Seniority.UNKNOWN);
        assertThat(SeniorityParser.parse("")).isEqualTo(Seniority.UNKNOWN);
    }
}
