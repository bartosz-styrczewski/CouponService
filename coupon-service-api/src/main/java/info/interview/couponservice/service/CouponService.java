package info.interview.couponservice.service;

/**
 * Manages discount coupons.
 */
public interface CouponService {

    /**
     * Creates a new coupon. Coupon codes are case-insensitive: they are stored in upper case,
     * so {@code wiosna} and {@code WIOSNA} are the same code.
     *
     * @throws CouponAlreadyExistsException if a coupon with the same code already exists
     */
    Coupon createCoupon(CreateCouponCommand command);
}
