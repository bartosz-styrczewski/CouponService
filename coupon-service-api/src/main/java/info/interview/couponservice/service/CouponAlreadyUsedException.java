package info.interview.couponservice.service;

public class CouponAlreadyUsedException extends RuntimeException {

    public CouponAlreadyUsedException(String code, String userId) {
        super("User " + userId + " has already used coupon " + code);
    }
}
