package info.interview.couponservice.geolocation.impl;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GeolocationConfigurationTest {

    private static final Map<String, GeolocationProperties.Provider> ALL_URLS = Map.of(
            "country-is", new GeolocationProperties.Provider("https://api.country.is"),
            "geojs", new GeolocationProperties.Provider("https://get.geojs.io"));

    @Test
    void createsProvidersInConfiguredOrder() {
        List<GeolocationProvider> providers = providers(List.of("geojs", "country-is"), ALL_URLS);

        assertThat(providers).extracting(GeolocationProvider::name).containsExactly("geojs", "country-is");
    }

    @Test
    void createsSingleProvider() {
        List<GeolocationProvider> providers = providers(
                List.of("country-is"),
                Map.of("country-is", new GeolocationProperties.Provider("https://api.country.is")));

        assertThat(providers).extracting(GeolocationProvider::name).containsExactly("country-is");
    }

    @Test
    void rejectsUnknownProvider() {
        assertThatThrownBy(() -> providers(List.of("country-is", "xyz"), ALL_URLS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Unknown geolocation provider 'xyz', available: country-is, geojs");
    }

    @Test
    void rejectsProviderWithoutUrl() {
        assertThatThrownBy(() -> providers(
                List.of("geojs"),
                Map.of("country-is", new GeolocationProperties.Provider("https://api.country.is"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Missing coupon.geolocation.provider.geojs.url");
    }

    @Test
    void rejectsEmptyProviderList() {
        assertThatThrownBy(() -> providers(List.of(), ALL_URLS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No geolocation provider");
    }

    private static List<GeolocationProvider> providers(
            List<String> names, Map<String, GeolocationProperties.Provider> urls) {
        GeolocationProperties properties = new GeolocationProperties(
                names,
                Duration.ofSeconds(1),
                Duration.ofSeconds(2),
                new GeolocationProperties.Retry(2, Duration.ofMillis(200)),
                urls);
        return GeolocationConfiguration.providers(properties, RestClient.builder());
    }
}
