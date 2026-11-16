package info.interview.couponservice.geolocation.impl;

import info.interview.couponservice.geolocation.GeolocationService;
import info.interview.couponservice.geolocation.GeolocationUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Asks the providers one by one and returns the first answer. Each provider is retried
 * after timeouts and server errors before the next one is asked.
 */
class FallbackGeolocationService implements GeolocationService {

    private static final Logger log = LoggerFactory.getLogger(FallbackGeolocationService.class);

    private final List<GeolocationProvider> providers;

    private final RetryTemplate retryTemplate;

    FallbackGeolocationService(List<GeolocationProvider> providers, long maxRetries, Duration retryDelay) {
        this.providers = List.copyOf(providers);
        this.retryTemplate = new RetryTemplate(RetryPolicy.builder()
                .maxRetries(maxRetries)
                .delay(retryDelay)
                .includes(ResourceAccessException.class, HttpServerErrorException.class)
                .build());
    }

    @Override
    @Cacheable(cacheNames = "countries", unless = "#result == null")
    public Optional<String> findCountry(String ipAddress) {
        if (!isPublic(ipAddress)) {
            return Optional.empty();
        }
        for (GeolocationProvider provider : providers) {
            try {
                return retryTemplate.execute(() -> provider.findCountry(ipAddress))
                        .map(country -> country.toUpperCase(Locale.ROOT));
            } catch (RetryException exception) {
                log.warn("Geolocation provider {} failed: {}", provider.name(), exception.getLastException().toString());
            }
        }
        throw new GeolocationUnavailableException();
    }

    private static boolean isPublic(String ipAddress) {
        try {
            InetAddress address = InetAddress.ofLiteral(ipAddress);
            return !(address.isLoopbackAddress()
                    || address.isSiteLocalAddress()
                    || address.isLinkLocalAddress()
                    || address.isAnyLocalAddress());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
