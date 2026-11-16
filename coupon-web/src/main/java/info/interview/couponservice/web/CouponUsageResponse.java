package info.interview.couponservice.web;

import info.interview.couponservice.service.CouponUsage;

import java.time.Instant;

record CouponUsageResponse(String code, String userId, Instant usedAt) {

    static CouponUsageResponse from(CouponUsage usage) {
        return new CouponUsageResponse(usage.code(), usage.userId(), usage.usedAt());
    }
}
