package info.interview.couponservice.service;

public class CouponExhaustedException extends RuntimeException {

    public CouponExhaustedException(String code) {
        super("Coupon " + code + " has reached its maximum number of uses");
    }
}
