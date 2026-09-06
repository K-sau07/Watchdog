package com.watchdog.domain.model;

/**
 * Employment type from the ATS field or inferred (spec §5).
 * UNKNOWN when neither the field nor the title/body makes it clear.
 */
public enum EmploymentType {
    FULL_TIME,
    INTERNSHIP,
    CONTRACT,
    PART_TIME,
    UNKNOWN
}
