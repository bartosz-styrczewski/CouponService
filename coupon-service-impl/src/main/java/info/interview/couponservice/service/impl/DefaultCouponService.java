package info.interview.couponservice.service.impl;

import info.interview.couponservice.persistence.CouponEntity;
import info.interview.couponservice.persistence.CouponRepository;
import info.interview.couponservice.persistence.CouponUsageEntity;
import info.interview.couponservice.persistence.CouponUsageRepository;
import info.interview.couponservice.service.Coupon;
import info.interview.couponservice.service.CouponAlreadyExistsException;
import info.interview.couponservice.service.CouponAlreadyUsedException;
import info.interview.couponservice.service.CouponExhaustedException;
import info.interview.couponservice.service.CouponNotFoundException;
import info.interview.couponservice.service.CouponService;
import info.interview.couponservice.service.CouponUsage;
import info.interview.couponservice.service.CreateCouponCommand;
import info.interview.couponservice.service.RegisterCouponUsageCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

@Service
class DefaultCouponService implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCouponService.class);

    private final CouponRepository couponRepository;

    private final CouponUsageRepository couponUsageRepository;

    DefaultCouponService(CouponRepository couponRepository, CouponUsageRepository couponUsageRepository) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
    }

    @Override
    public Coupon createCoupon(CreateCouponCommand command) {
        String code = CouponCodes.normalize(command.code());
        CouponEntity coupon = new CouponEntity(
                code,
                command.country().toUpperCase(Locale.ROOT),
                command.maxUses(),
                now());
        try {
            CouponEntity savedCoupon = couponRepository.saveAndFlush(coupon);
            log.info("Created coupon {}", code);
            return toCoupon(savedCoupon);
        } catch (DataIntegrityViolationException exception) {
            // Request validation covers all other columns, so only the unique code constraint can fail here.
            throw new CouponAlreadyExistsException(code, exception);
        }
    }

    @Override
    @Transactional
    public CouponUsage registerUsage(RegisterCouponUsageCommand command) {
        String code = CouponCodes.normalize(command.code());
        CouponEntity coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new CouponNotFoundException(code));
        if (couponUsageRepository.existsByCouponIdAndUserId(coupon.getId(), command.userId())) {
            throw new CouponAlreadyUsedException(code, command.userId());
        }
        if (coupon.getCurrentUses() >= coupon.getMaxUses()) {
            throw new CouponExhaustedException(code);
        }
        coupon.incrementCurrentUses();
        CouponUsageEntity usage = couponUsageRepository.save(
                new CouponUsageEntity(coupon.getId(), command.userId(), now()));
        log.info("User {} used coupon {}", command.userId(), code);
        return new CouponUsage(code, usage.getUserId(), usage.getUsedAt());
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
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
