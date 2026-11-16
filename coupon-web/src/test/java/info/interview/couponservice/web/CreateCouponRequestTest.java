package info.interview.couponservice.web;

import info.interview.couponservice.service.CreateCouponCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class CreateCouponRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        assertThat(violatedFields(new CreateCouponRequest("Wiosna-2026_PL", 100, "PL"))).isEmpty();
    }

    @Test
    void acceptsCodeWith32Characters() {
        assertThat(violatedFields(new CreateCouponRequest("A".repeat(32), 100, "PL"))).isEmpty();
    }

    @Test
    void rejectsCodeLongerThan32Characters() {
        assertThat(violatedFields(new CreateCouponRequest("A".repeat(33), 100, "PL"))).containsExactly("code");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not valid!", "zniżka", "a b"})
    void rejectsCodeWithForbiddenCharacters(String code) {
        assertThat(violatedFields(new CreateCouponRequest(code, 100, "PL"))).containsExactly("code");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsMissingCode(String code) {
        assertThat(violatedFields(new CreateCouponRequest(code, 100, "PL"))).containsExactly("code");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsMaxUsesLowerThanOne(int maxUses) {
        assertThat(violatedFields(new CreateCouponRequest("WIOSNA", maxUses, "PL"))).containsExactly("maxUses");
    }

    @Test
    void rejectsMissingMaxUses() {
        assertThat(violatedFields(new CreateCouponRequest("WIOSNA", null, "PL"))).containsExactly("maxUses");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "XX")
    void rejectsMissingOrUnknownCountry(String country) {
        assertThat(violatedFields(new CreateCouponRequest("WIOSNA", 100, country))).containsExactly("country");
    }

    @Test
    void toCommandCopiesAllFields() {
        CreateCouponCommand command = new CreateCouponRequest("WIOSNA", 100, "PL").toCommand();

        assertThat(command).isEqualTo(new CreateCouponCommand("WIOSNA", 100, "PL"));
    }

    private Set<String> violatedFields(CreateCouponRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
