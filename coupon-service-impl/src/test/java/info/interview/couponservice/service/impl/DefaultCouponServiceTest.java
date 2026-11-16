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
import info.interview.couponservice.service.CouponUsage;
import info.interview.couponservice.service.CreateCouponCommand;
import info.interview.couponservice.service.RegisterCouponUsageCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionOperations;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCouponServiceTest {

    private static final UUID COUPON_ID = UUID.randomUUID();

    private static final String POLISH_IP = "83.31.0.1";

    private final CouponRepository couponRepository = mock(CouponRepository.class);

    private final CouponUsageRepository couponUsageRepository = mock(CouponUsageRepository.class);

    private final GeolocationService geolocationService = mock(GeolocationService.class);

    private final DefaultCouponService couponService = new DefaultCouponService(
            couponRepository, couponUsageRepository, geolocationService, TransactionOperations.withoutTransaction());

    @Test
    void createCouponSavesCodeAndCountryInUpperCase() {
        when(couponRepository.saveAndFlush(any(CouponEntity.class))).then(returnsFirstArg());

        couponService.createCoupon(new CreateCouponCommand("wiosna", 100, "pl"));

        ArgumentCaptor<CouponEntity> savedCoupon = ArgumentCaptor.forClass(CouponEntity.class);
        verify(couponRepository).saveAndFlush(savedCoupon.capture());
        assertThat(savedCoupon.getValue().getCode()).isEqualTo("WIOSNA");
        assertThat(savedCoupon.getValue().getCountry()).isEqualTo("PL");
        assertThat(savedCoupon.getValue().getMaxUses()).isEqualTo(100);
        assertThat(savedCoupon.getValue().getCurrentUses()).isZero();
        assertThat(savedCoupon.getValue().getCreatedAt()).isNotNull();
    }

    @Test
    void createCouponReturnsSavedCoupon() {
        when(couponRepository.saveAndFlush(any(CouponEntity.class))).then(returnsFirstArg());

        Coupon coupon = couponService.createCoupon(new CreateCouponCommand("wiosna", 100, "pl"));

        assertThat(coupon.code()).isEqualTo("WIOSNA");
        assertThat(coupon.country()).isEqualTo("PL");
        assertThat(coupon.maxUses()).isEqualTo(100);
        assertThat(coupon.currentUses()).isZero();
        assertThat(coupon.createdAt()).isNotNull();
    }

    @Test
    void createCouponThrowsCouponAlreadyExistsWhenCodeIsTaken() {
        when(couponRepository.saveAndFlush(any(CouponEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> couponService.createCoupon(new CreateCouponCommand("wiosna", 100, "pl")))
                .isInstanceOf(CouponAlreadyExistsException.class)
                .hasMessageContaining("WIOSNA")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void registerUsageSavesUsageAndCountsIt() {
        existingPolishCoupon();
        when(geolocationService.findCountry(POLISH_IP)).thenReturn(Optional.of("PL"));
        when(couponUsageRepository.saveAndFlush(any(CouponUsageEntity.class))).then(returnsFirstArg());
        when(couponRepository.incrementCurrentUsesIfAvailable(COUPON_ID)).thenReturn(1);

        CouponUsage usage = couponService.registerUsage(new RegisterCouponUsageCommand("wiosna", "user-1", POLISH_IP));

        ArgumentCaptor<CouponUsageEntity> savedUsage = ArgumentCaptor.forClass(CouponUsageEntity.class);
        verify(couponUsageRepository).saveAndFlush(savedUsage.capture());
        assertThat(savedUsage.getValue().getCouponId()).isEqualTo(COUPON_ID);
        assertThat(savedUsage.getValue().getUserId()).isEqualTo("user-1");
        assertThat(savedUsage.getValue().getUsedAt()).isNotNull();
        verify(couponRepository).incrementCurrentUsesIfAvailable(COUPON_ID);
        assertThat(usage.code()).isEqualTo("WIOSNA");
        assertThat(usage.userId()).isEqualTo("user-1");
        assertThat(usage.usedAt()).isNotNull();
    }

    @Test
    void registerUsageThrowsCouponNotFoundForUnknownCode() {
        when(couponRepository.findByCode("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("unknown", "user-1", POLISH_IP)))
                .isInstanceOf(CouponNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
        verifyNothingSaved();
    }

    @Test
    void registerUsageThrowsCountryNotAllowedWhenRequestComesFromOtherCountry() {
        existingPolishCoupon();
        when(geolocationService.findCountry("85.214.0.1")).thenReturn(Optional.of("DE"));

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("WIOSNA", "user-1", "85.214.0.1")))
                .isInstanceOf(CountryNotAllowedException.class)
                .hasMessageContaining("DE");
        verifyNothingSaved();
    }

    @Test
    void registerUsageThrowsCountryNotAllowedWhenCountryIsUnknown() {
        existingPolishCoupon();
        when(geolocationService.findCountry("127.0.0.1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("WIOSNA", "user-1", "127.0.0.1")))
                .isInstanceOf(CountryNotAllowedException.class)
                .hasMessageContaining("unknown country");
        verifyNothingSaved();
    }

    @Test
    void registerUsageThrowsCountryCheckUnavailableWhenGeolocationFails() {
        existingPolishCoupon();
        when(geolocationService.findCountry(POLISH_IP)).thenThrow(new GeolocationUnavailableException());

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("WIOSNA", "user-1", POLISH_IP)))
                .isInstanceOf(CountryCheckUnavailableException.class)
                .hasCauseInstanceOf(GeolocationUnavailableException.class);
        verifyNothingSaved();
    }

    @Test
    void registerUsageThrowsCouponAlreadyUsedWhenUsageOfUserAlreadyExists() {
        existingPolishCoupon();
        when(geolocationService.findCountry(POLISH_IP)).thenReturn(Optional.of("PL"));
        when(couponUsageRepository.saveAndFlush(any(CouponUsageEntity.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("WIOSNA", "user-1", POLISH_IP)))
                .isInstanceOf(CouponAlreadyUsedException.class)
                .hasMessageContaining("user-1")
                .hasCauseInstanceOf(DataIntegrityViolationException.class);
        verify(couponRepository, never()).incrementCurrentUsesIfAvailable(any());
    }

    @Test
    void registerUsageThrowsCouponExhaustedWhenNoUseIsLeft() {
        existingPolishCoupon();
        when(geolocationService.findCountry(POLISH_IP)).thenReturn(Optional.of("PL"));
        when(couponUsageRepository.saveAndFlush(any(CouponUsageEntity.class))).then(returnsFirstArg());
        when(couponRepository.incrementCurrentUsesIfAvailable(COUPON_ID)).thenReturn(0);

        assertThatThrownBy(() -> couponService.registerUsage(
                new RegisterCouponUsageCommand("WIOSNA", "user-2", POLISH_IP)))
                .isInstanceOf(CouponExhaustedException.class)
                .hasMessageContaining("WIOSNA");
    }

    private void existingPolishCoupon() {
        CouponEntity coupon = new CouponEntity("WIOSNA", "PL", 10, Instant.now());
        ReflectionTestUtils.setField(coupon, "id", COUPON_ID);
        when(couponRepository.findByCode("WIOSNA")).thenReturn(Optional.of(coupon));
    }

    private void verifyNothingSaved() {
        verify(couponUsageRepository, never()).saveAndFlush(any());
        verify(couponRepository, never()).incrementCurrentUsesIfAvailable(any());
    }
}
