package info.interview.couponservice.service;

public class CouponAlreadyExistsException extends RuntimeException {

    public CouponAlreadyExistsException(String code, Throwable cause) {
        super("Coupon with code " + code + " already exists", cause);
    }
}
