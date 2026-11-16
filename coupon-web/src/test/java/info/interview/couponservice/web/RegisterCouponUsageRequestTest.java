package info.interview.couponservice.web;

import info.interview.couponservice.service.RegisterCouponUsageCommand;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterCouponUsageRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequest() {
        assertThat(violatedFields(new RegisterCouponUsageRequest("user-1"))).isEmpty();
    }

    @Test
    void acceptsUserIdWith64Characters() {
        assertThat(violatedFields(new RegisterCouponUsageRequest("u".repeat(64)))).isEmpty();
    }

    @Test
    void rejectsUserIdLongerThan64Characters() {
        assertThat(violatedFields(new RegisterCouponUsageRequest("u".repeat(65)))).containsExactly("userId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void rejectsMissingUserId(String userId) {
        assertThat(violatedFields(new RegisterCouponUsageRequest(userId))).containsExactly("userId");
    }

    @Test
    void toCommandCombinesCodeAndUserId() {
        RegisterCouponUsageCommand command = new RegisterCouponUsageRequest("user-1").toCommand("WIOSNA");

        assertThat(command).isEqualTo(new RegisterCouponUsageCommand("WIOSNA", "user-1"));
    }

    private Set<String> violatedFields(RegisterCouponUsageRequest request) {
        return validator.validate(request).stream()
                .map(violation -> violation.getPropertyPath().toString())
                .collect(Collectors.toSet());
    }
}
