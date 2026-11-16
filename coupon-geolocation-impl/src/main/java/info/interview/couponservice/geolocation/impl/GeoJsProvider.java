package info.interview.couponservice.geolocation.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Client of <a href="https://www.geojs.io">GeoJS</a>.
 */
class GeoJsProvider implements GeolocationProvider {

    static final String NAME = "geojs";

    private final RestClient restClient;

    GeoJsProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<String> findCountry(String ipAddress) {
        GeoJsResponse response = restClient.get()
                .uri("/v1/ip/country/{ipAddress}.json", ipAddress)
                .retrieve()
                .body(GeoJsResponse.class);
        return Optional.ofNullable(response)
                .map(GeoJsResponse::country)
                .filter(country -> !country.isBlank());
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeoJsResponse(String country) {
    }
}
