package info.interview.couponservice.web;

import info.interview.couponservice.service.Coupon;

import java.time.Instant;

record CouponResponse(String code, String country, int maxUses, int currentUses, Instant createdAt) {

    static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.code(),
                coupon.country(),
                coupon.maxUses(),
                coupon.currentUses(),
                coupon.createdAt());
    }
}
