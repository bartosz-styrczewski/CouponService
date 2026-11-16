package info.interview.couponservice.service;

public class CountryCheckUnavailableException extends RuntimeException {

    public CountryCheckUnavailableException(String code, Throwable cause) {
        super("The country of the request cannot be checked for coupon " + code + " right now", cause);
    }
}
