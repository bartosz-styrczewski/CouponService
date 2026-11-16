package info.interview.couponservice.web;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CountryCodeValidatorTest {

    private final CountryCodeValidator validator = new CountryCodeValidator();

    @ParameterizedTest
    @ValueSource(strings = {"PL", "pl", "De"})
    void acceptsIsoCountryCodeInAnyLetterCase(String countryCode) {
        assertThat(validator.isValid(countryCode, null)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"XX", "POL", "P"})
    void rejectsUnknownCountryCode(String countryCode) {
        assertThat(validator.isValid(countryCode, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void leavesMissingValueToNotBlank(String countryCode) {
        assertThat(validator.isValid(countryCode, null)).isTrue();
    }
}
