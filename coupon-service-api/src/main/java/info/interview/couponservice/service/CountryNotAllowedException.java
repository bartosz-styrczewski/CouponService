package info.interview.couponservice.service;

public class CountryNotAllowedException extends RuntimeException {

    public CountryNotAllowedException(String code, String allowedCountry, String requestCountry) {
        super("Coupon %s can only be used in %s, but the request comes from %s"
                .formatted(code, allowedCountry, requestCountry != null ? requestCountry : "an unknown country"));
    }
}
