package info.interview.couponservice.geolocation.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * @param providers names of the providers to use, in priority order: the first one is asked first,
 *                  the next ones only when the previous ones fail
 * @param provider  settings of each provider by its name
 */
@ConfigurationProperties("coupon.geolocation")
record GeolocationProperties(
        List<String> providers,
        Duration connectTimeout,
        Duration readTimeout,
        Retry retry,
        Map<String, Provider> provider) {

    GeolocationProperties {
        providers = providers == null ? List.of() : List.copyOf(providers);
        provider = provider == null ? Map.of() : Map.copyOf(provider);
    }

    record Retry(long maxRetries, Duration delay) {
    }

    record Provider(String url) {
    }
}
