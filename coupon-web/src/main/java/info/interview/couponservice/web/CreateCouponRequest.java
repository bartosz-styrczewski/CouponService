package info.interview.couponservice.web;

import info.interview.couponservice.service.CreateCouponCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

record CreateCouponRequest(
        @NotBlank @Size(max = 32) @Pattern(regexp = "[A-Za-z0-9_-]*") String code,
        @NotNull @Positive Integer maxUses,
        @NotBlank @CountryCode String country) {

    CreateCouponCommand toCommand() {
        return new CreateCouponCommand(code, maxUses, country);
    }
}
