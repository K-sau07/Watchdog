package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SoftwareRoleMatcherTest {

    @Test
    void keepsCoreSoftwareRoles() {
        for (String t : new String[]{
                "Software Engineer", "Senior Software Engineer", "Staff Software Engineer",
                "Software Engineer 3", "Software Engineer II", "Backend Engineer",
                "Frontend Engineer", "Full Stack Engineer", "Full-Stack Developer",
                "Data Engineer", "Machine Learning Engineer", "ML Engineer", "AI Engineer",
                "Data Scientist", "Site Reliability Engineer", "Staff Site Reliability Engineer",
                "Platform Engineer", "Infrastructure Engineer", "Security Engineer",
                "Mobile Engineer", "iOS Engineer", "Android Developer", "DevOps Engineer",
                "Member of Technical Staff", "Principal Engineer", "Software Developer",
                "Research Engineer", "Applied AI Engineer", "Forward Deployed Engineer",
                "Embedded Software Engineer", "Cloud Engineer", "Programmer"}) {
            assertThat(SoftwareRoleMatcher.isSoftwareRole(t)).as(t).isTrue();
        }
    }

    @Test
    void dropsNonSoftwareRolesThatBorrowEngWords() {
        for (String t : new String[]{
                "Solutions Architect", "Senior Solutions Architect", "Solutions Engineer",
                "Sales Engineer", "Enterprise Sales Engineer", "Pre-Sales Engineer",
                "GTM Engineer", "Customer Success Engineer", "Technical Services Engineer",
                "Support Engineer", "Field Engineer", "Design Engineer", "Hardware Engineer",
                "Mechanical Engineer", "Network Engineer", "QA Engineer", "Test Engineer",
                "Developer Advocate", "Developer Educator", "Account Executive",
                "Business Development Manager", "Marketing Manager"}) {
            assertThat(SoftwareRoleMatcher.isSoftwareRole(t)).as(t).isFalse();
        }
    }

    @Test
    void excludeWinsOverIncludeOnHybridTitles() {
        // "Solutions Engineer" contains "engineer" but is sales — must drop.
        assertThat(SoftwareRoleMatcher.isSoftwareRole("Solutions Engineer")).isFalse();
        // But a real software role with a sales-y sub-team still counts (software word wins,
        // no EXCLUDE token): "Backend Software Engineer, GTM Innovation".
        assertThat(SoftwareRoleMatcher.isSoftwareRole("Backend Software Engineer, GTM Innovation")).isTrue();
    }

    @Test
    void dropsUnrelatedAndBlank() {
        assertThat(SoftwareRoleMatcher.isSoftwareRole("Product Manager")).isFalse();
        assertThat(SoftwareRoleMatcher.isSoftwareRole("Recruiter")).isFalse();
        assertThat(SoftwareRoleMatcher.isSoftwareRole("Chef")).isFalse();
        assertThat(SoftwareRoleMatcher.isSoftwareRole(null)).isFalse();
        assertThat(SoftwareRoleMatcher.isSoftwareRole("")).isFalse();
    }
}
