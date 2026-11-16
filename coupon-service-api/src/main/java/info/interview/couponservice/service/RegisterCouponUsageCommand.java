package info.interview.couponservice.service;

public record RegisterCouponUsageCommand(String code, String userId, String ipAddress) {
}
