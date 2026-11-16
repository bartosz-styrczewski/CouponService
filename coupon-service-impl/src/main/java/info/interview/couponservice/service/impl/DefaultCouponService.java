package info.interview.couponservice.service.impl;

import info.interview.couponservice.persistence.CouponEntity;
import info.interview.couponservice.persistence.CouponRepository;
import info.interview.couponservice.service.Coupon;
import info.interview.couponservice.service.CouponAlreadyExistsException;
import info.interview.couponservice.service.CouponService;
import info.interview.couponservice.service.CreateCouponCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
class DefaultCouponService implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCouponService.class);

    private final CouponRepository couponRepository;

    DefaultCouponService(CouponRepository couponRepository) {
        this.couponRepository = couponRepository;
    }

    @Override
    public Coupon createCoupon(CreateCouponCommand command) {
        String code = CouponCodes.normalize(command.code());
        CouponEntity coupon = new CouponEntity(
                code,
                command.country().toUpperCase(Locale.ROOT),
                command.maxUses(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        try {
            CouponEntity savedCoupon = couponRepository.saveAndFlush(coupon);
            log.info("Created coupon {}", code);
            return toCoupon(savedCoupon);
        } catch (DataIntegrityViolationException exception) {
            // Request validation covers all other columns, so only the unique code constraint can fail here.
            throw new CouponAlreadyExistsException(code);
        }
    }

    private static Coupon toCoupon(CouponEntity coupon) {
        return new Coupon(
                coupon.getCode(),
                coupon.getCountry(),
                coupon.getMaxUses(),
                coupon.getCurrentUses(),
                coupon.getCreatedAt());
    }
}
