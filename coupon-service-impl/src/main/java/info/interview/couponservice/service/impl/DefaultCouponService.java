package info.interview.couponservice.service.impl;

import info.interview.couponservice.geolocation.GeolocationService;
import info.interview.couponservice.geolocation.GeolocationUnavailableException;
import info.interview.couponservice.persistence.CouponEntity;
import info.interview.couponservice.persistence.CouponRepository;
import info.interview.couponservice.persistence.CouponUsageEntity;
import info.interview.couponservice.persistence.CouponUsageRepository;
import info.interview.couponservice.service.CountryCheckUnavailableException;
import info.interview.couponservice.service.CountryNotAllowedException;
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
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;

@Service
class DefaultCouponService implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(DefaultCouponService.class);

    private final CouponRepository couponRepository;

    private final CouponUsageRepository couponUsageRepository;

    private final GeolocationService geolocationService;

    private final TransactionOperations transactionOperations;

    DefaultCouponService(
            CouponRepository couponRepository,
            CouponUsageRepository couponUsageRepository,
            GeolocationService geolocationService,
            TransactionOperations transactionOperations) {
        this.couponRepository = couponRepository;
        this.couponUsageRepository = couponUsageRepository;
        this.geolocationService = geolocationService;
        this.transactionOperations = transactionOperations;
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
    public CouponUsage registerUsage(RegisterCouponUsageCommand command) {
        CouponEntity coupon = findCoupon(CouponCodes.normalize(command.code()));
        // The country is checked outside the transaction, so waiting for geolocation does not hold a database connection.
        checkCountry(coupon, command.ipAddress());
        return transactionOperations.execute(status -> saveUsage(coupon, command.userId()));
    }

    private void checkCountry(CouponEntity coupon, String ipAddress) {
        Optional<String> requestCountry;
        try {
            requestCountry = geolocationService.findCountry(ipAddress);
        } catch (GeolocationUnavailableException exception) {
            throw new CountryCheckUnavailableException(coupon.getCode(), exception);
        }
        if (requestCountry.filter(coupon.getCountry()::equals).isEmpty()) {
            throw new CountryNotAllowedException(coupon.getCode(), coupon.getCountry(), requestCountry.orElse(null));
        }
    }

    private CouponUsage saveUsage(CouponEntity coupon, String userId) {
        CouponUsageEntity usage;
        try {
            usage = couponUsageRepository.saveAndFlush(new CouponUsageEntity(coupon.getId(), userId, now()));
        } catch (DataIntegrityViolationException exception) {
            // The unique constraint on coupon and user is the only one that can fail for a usage of an existing coupon.
            throw new CouponAlreadyUsedException(coupon.getCode(), userId, exception);
        }
        if (couponRepository.incrementCurrentUsesIfAvailable(coupon.getId()) == 0) {
            throw new CouponExhaustedException(coupon.getCode());
        }
        log.info("User {} used coupon {}", userId, coupon.getCode());
        return new CouponUsage(coupon.getCode(), usage.getUserId(), usage.getUsedAt());
    }

    private CouponEntity findCoupon(String code) {
        return couponRepository.findByCode(code).orElseThrow(() -> new CouponNotFoundException(code));
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
