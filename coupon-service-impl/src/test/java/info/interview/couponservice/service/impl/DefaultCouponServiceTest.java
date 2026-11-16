package info.interview.couponservice.service.impl;

import info.interview.couponservice.persistence.CouponEntity;
import info.interview.couponservice.persistence.CouponRepository;
import info.interview.couponservice.service.Coupon;
import info.interview.couponservice.service.CouponAlreadyExistsException;
import info.interview.couponservice.service.CreateCouponCommand;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultCouponServiceTest {

    private final CouponRepository couponRepository = mock(CouponRepository.class);

    private final DefaultCouponService couponService = new DefaultCouponService(couponRepository);

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
                .hasMessageContaining("WIOSNA");
    }
}
