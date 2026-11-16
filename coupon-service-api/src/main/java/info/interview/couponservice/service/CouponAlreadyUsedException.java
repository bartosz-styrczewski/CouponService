package info.interview.couponservice.service;

public class CouponAlreadyUsedException extends RuntimeException {

    public CouponAlreadyUsedException(String code, String userId, Throwable cause) {
        super("User " + userId + " has already used coupon " + code, cause);
    }
}
