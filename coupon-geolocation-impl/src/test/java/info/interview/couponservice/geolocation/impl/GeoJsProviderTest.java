package info.interview.couponservice.geolocation.impl;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GeoJsProviderTest {

    private final RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("https://get.geojs.io");

    private final MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();

    private final GeoJsProvider provider = new GeoJsProvider(restClientBuilder.build());

    @Test
    void returnsCountryFromResponse() {
        server.expect(requestTo("https://get.geojs.io/v1/ip/country/83.31.0.1.json"))
                .andRespond(withSuccess("""
                        {"country": "PL", "country_3": "POL", "ip": "83.31.0.1", "name": "Poland"}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findCountry("83.31.0.1")).contains("PL");
    }

    @Test
    void returnsEmptyResultWhenCountryIsBlank() {
        server.expect(requestTo("https://get.geojs.io/v1/ip/country/83.31.0.1.json"))
                .andRespond(withSuccess("""
                        {"country": "", "country_3": "", "ip": "83.31.0.1", "name": ""}
                        """, MediaType.APPLICATION_JSON));

        assertThat(provider.findCountry("83.31.0.1")).isEmpty();
    }

    @Test
    void throwsWhenProviderReturnsServerError() {
        server.expect(requestTo("https://get.geojs.io/v1/ip/country/83.31.0.1.json")).andRespond(withServerError());

        assertThatThrownBy(() -> provider.findCountry("83.31.0.1")).isInstanceOf(HttpServerErrorException.class);
    }
}
