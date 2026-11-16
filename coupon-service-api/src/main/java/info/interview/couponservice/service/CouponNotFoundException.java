package info.interview.couponservice.service;

public class CouponNotFoundException extends RuntimeException {

    public CouponNotFoundException(String code) {
        super("Coupon with code " + code + " does not exist");
    }
}
