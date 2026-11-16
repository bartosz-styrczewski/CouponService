package info.interview.couponservice.geolocation.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class CountryIsProviderTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://api.country.is");

    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

    private final CountryIsProvider provider = new CountryIsProvider(restClientBuilder.build());

    @Test
    void returnsCountryFromResponse() {
        server.expect(requestTo("https://api.country.is/83.31.0.1"))
                .andRespond(withSuccess("""
                        {"ip": "83.31.0.1", "country": "PL"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findCountry("83.31.0.1")).contains("PL");
    }

    @Test
    void returnsEmptyResultWhenAddressIsNotFound() {
        server.expect(requestTo("https://api.country.is/83.31.0.1")).andRespond(withResourceNotFound());

        assertThat(provider.findCountry("83.31.0.1")).isEmpty();
    }

    @Test
    void throwsWhenProviderReturnsServerError() {
        server.expect(requestTo("https://api.country.is/83.31.0.1")).andRespond(withServerError());

        assertThatThrownBy(() -> provider.findCountry("83.31.0.1")).isInstanceOf(HttpServerErrorException.class);
    }
}
