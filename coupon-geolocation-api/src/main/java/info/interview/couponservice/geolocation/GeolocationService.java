package info.interview.couponservice.geolocation;

import java.util.Optional;

/**
 * Finds the country of an IP address.
 */
public interface GeolocationService {

    /**
     * Returns the ISO 3166-1 alpha-2 code of the country of the IP address, for example {@code PL},
     * or an empty result if the country cannot be determined, for example for private addresses.
     *
     * @throws GeolocationUnavailableException if no geolocation provider could answer
     */
    Optional<String> findCountry(String ipAddress);
}
