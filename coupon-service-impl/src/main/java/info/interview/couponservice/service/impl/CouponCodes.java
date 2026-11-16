package info.interview.couponservice.service.impl;

import java.util.Locale;

final class CouponCodes {

    private CouponCodes() {
    }

    static String normalize(String code) {
        return code.toUpperCase(Locale.ROOT);
    }
}
