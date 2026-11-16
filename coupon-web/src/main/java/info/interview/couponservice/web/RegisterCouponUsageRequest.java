package info.interview.couponservice.web;

import info.interview.couponservice.service.RegisterCouponUsageCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterCouponUsageRequest(@NotBlank @Size(max = 64) String userId) {

    RegisterCouponUsageCommand toCommand(String code) {
        return new RegisterCouponUsageCommand(code, userId);
    }
}
