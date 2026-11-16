package info.interview.couponservice.service;

public record CreateCouponCommand(String code, int maxUses, String country) {
}
