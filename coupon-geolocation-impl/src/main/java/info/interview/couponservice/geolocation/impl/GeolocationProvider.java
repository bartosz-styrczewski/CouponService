package info.interview.couponservice.geolocation.impl;

import java.util.Optional;

interface GeolocationProvider {

    Optional<String> findCountry(String ipAddress);

    default String name() {
        return getClass().getSimpleName();
    }
}
