package info.interview.couponservice.service;

import java.time.Instant;

public record CouponUsage(String code, String userId, Instant usedAt) {
}
