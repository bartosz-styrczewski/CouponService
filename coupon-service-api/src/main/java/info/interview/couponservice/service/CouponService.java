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

    /**
     * Registers that a user has used a coupon. The coupon code is case-insensitive.
     * The coupon can only be used from its country, which is determined from the IP address of the request.
     *
     * @throws CouponNotFoundException          if there is no coupon with the given code
     * @throws CountryNotAllowedException       if the request comes from another or an unknown country
     * @throws CountryCheckUnavailableException if the country of the request cannot be checked
     * @throws CouponAlreadyUsedException       if the user has already used this coupon
     * @throws CouponExhaustedException         if the coupon has reached its maximum number of uses
     */
    CouponUsage registerUsage(RegisterCouponUsageCommand command);
}
