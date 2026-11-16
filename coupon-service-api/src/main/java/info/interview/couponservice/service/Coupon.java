package info.interview.couponservice.service;

import java.time.Instant;

public record Coupon(String code, String country, int maxUses, int currentUses, Instant createdAt) {
}
