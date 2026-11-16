package info.interview.couponservice.geolocation.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

/**
 * Client of <a href="https://country.is">country.is</a>.
 */
class CountryIsProvider implements GeolocationProvider {

    static final String NAME = "country-is";

    private final RestClient restClient;

    CountryIsProvider(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public Optional<String> findCountry(String ipAddress) {
        try {
            CountryIsResponse response = restClient.get()
                    .uri("/{ipAddress}", ipAddress)
                    .retrieve()
                    .body(CountryIsResponse.class);
            return Optional.ofNullable(response)
                    .map(CountryIsResponse::country)
                    .filter(country -> !country.isBlank());
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CountryIsResponse(String country) {
    }
}
