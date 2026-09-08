package com.watchdog.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SalaryParserTest {

    @Test
    void parsesDollarCommaRange() {
        Salary s = SalaryParser.parse("The salary range is $120,000 - $150,000 per year.");
        assertThat(s.min()).isEqualByComparingTo("120000");
        assertThat(s.max()).isEqualByComparingTo("150000");
    }

    @Test
    void parsesKSuffixRange() {
        Salary s = SalaryParser.parse("Compensation: $120k - $150k");
        assertThat(s.min()).isEqualByComparingTo("120000");
        assertThat(s.max()).isEqualByComparingTo("150000");
    }

    @Test
    void parsesEnDashRange() {
        Salary s = SalaryParser.parse("We offer $95,000–$130,000 depending on experience.");
        assertThat(s.min()).isEqualByComparingTo("95000");
        assertThat(s.max()).isEqualByComparingTo("130000");
    }

    @Test
    void parsesRangeWithExplicitCurrency() {
        Salary s = SalaryParser.parse("Base pay 120000-150000 USD");
        assertThat(s.min()).isEqualByComparingTo("120000");
        assertThat(s.max()).isEqualByComparingTo("150000");
        assertThat(s.currency()).isEqualTo("USD");
    }

    @Test
    void picksUpNonUsdCurrency() {
        Salary s = SalaryParser.parse("$110,000 to $145,000 CAD");
        assertThat(s.currency()).isEqualTo("CAD");
    }

    @Test
    void singleDollarAmountBecomesMinOnly() {
        Salary s = SalaryParser.parse("Pay: $150k");
        assertThat(s.min()).isEqualByComparingTo("150000");
        assertThat(s.max()).isNull();
    }

    @Test
    void dollarButNoExplicitCurrencyLeavesCurrencyNull() {
        // '$' is ambiguous across USD/CAD/AUD — we do not guess.
        Salary s = SalaryParser.parse("$120,000 - $150,000");
        assertThat(s.currency()).isNull();
    }

    // --- honesty guards: must NOT invent salaries ---

    @Test
    void rejectsHeadcountsAndYears() {
        assertThat(SalaryParser.parse("We have 500 employees across 3 offices.").isEmpty()).isTrue();
        assertThat(SalaryParser.parse("Founded in 2015, we serve 2 million users.").isEmpty()).isTrue();
    }

    @Test
    void rejectsBareRangeWithoutMoneySignal() {
        // No $/k/currency — indistinguishable from a non-salary range.
        assertThat(SalaryParser.parse("We grew revenue from 100,000 to 140,000 last year.").isEmpty())
                .isTrue();
    }

    @Test
    void rejectsHourlyRateAsAnnual() {
        // Known v1 limitation: an hourly figure is rejected, not annualized.
        assertThat(SalaryParser.parse("$50/hr contract role").isEmpty()).isTrue();
    }

    @Test
    void emptyWhenNoSalaryLanguage() {
        assertThat(SalaryParser.parse("Join our team building great products.").isEmpty()).isTrue();
        assertThat(SalaryParser.parse(null).isEmpty()).isTrue();
        assertThat(SalaryParser.parse("").isEmpty()).isTrue();
    }
}
