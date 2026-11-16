package info.interview.couponservice.service.impl;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CouponCodesTest {

    @Test
    void normalizeConvertsCodeToUpperCase() {
        assertThat(CouponCodes.normalize("Wiosna")).isEqualTo("WIOSNA");
    }
}
