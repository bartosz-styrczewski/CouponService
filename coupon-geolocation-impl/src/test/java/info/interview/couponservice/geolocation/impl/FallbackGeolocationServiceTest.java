package info.interview.couponservice.geolocation.impl;

import info.interview.couponservice.geolocation.GeolocationUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FallbackGeolocationServiceTest {

    private static final String PUBLIC_IP = "83.31.0.1";

    private final AtomicInteger primaryCalls = new AtomicInteger();

    private final AtomicInteger secondaryCalls = new AtomicInteger();

    @Test
    void returnsCountryFromPrimaryProvider() {
        FallbackGeolocationService service = service(
                ipAddress -> answer(primaryCalls, "PL"),
                ipAddress -> answer(secondaryCalls, "DE"));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
        assertThat(secondaryCalls).hasValue(0);
    }

    @Test
    void returnsCountryInUpperCase() {
        FallbackGeolocationService service = service(
                ipAddress -> answer(primaryCalls, "pl"),
                ipAddress -> answer(secondaryCalls, "DE"));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
    }

    @Test
    void retriesPrimaryProviderAfterServerError() {
        FallbackGeolocationService service = service(
                ipAddress -> primaryCalls.incrementAndGet() == 1 ? serverError() : Optional.of("PL"),
                ipAddress -> answer(secondaryCalls, "DE"));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
        assertThat(primaryCalls).hasValue(2);
        assertThat(secondaryCalls).hasValue(0);
    }

    @Test
    void usesSecondaryProviderWhenPrimaryKeepsFailing() {
        FallbackGeolocationService service = service(
                ipAddress -> {
                    primaryCalls.incrementAndGet();
                    return serverError();
                },
                ipAddress -> answer(secondaryCalls, "PL"));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
        assertThat(primaryCalls).hasValue(3);
        assertThat(secondaryCalls).hasValue(1);
    }

    @Test
    void doesNotRetryClientErrors() {
        FallbackGeolocationService service = service(
                ipAddress -> {
                    primaryCalls.incrementAndGet();
                    throw new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS);
                },
                ipAddress -> answer(secondaryCalls, "PL"));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
        assertThat(primaryCalls).hasValue(1);
    }

    @Test
    void returnsCountryFromOnlyProvider() {
        FallbackGeolocationService service = new FallbackGeolocationService(
                List.of(ipAddress -> answer(primaryCalls, "PL")), 2, Duration.ofMillis(1));

        assertThat(service.findCountry(PUBLIC_IP)).contains("PL");
    }

    @Test
    void throwsGeolocationUnavailableWhenOnlyProviderFails() {
        FallbackGeolocationService service = new FallbackGeolocationService(
                List.of(ipAddress -> {
                    primaryCalls.incrementAndGet();
                    return serverError();
                }),
                2,
                Duration.ofMillis(1));

        assertThatThrownBy(() -> service.findCountry(PUBLIC_IP))
                .isInstanceOf(GeolocationUnavailableException.class);
        assertThat(primaryCalls).hasValue(3);
    }

    @Test
    void throwsGeolocationUnavailableWhenAllProvidersFail() {
        FallbackGeolocationService service = service(
                ipAddress -> serverError(),
                ipAddress -> serverError());

        assertThatThrownBy(() -> service.findCountry(PUBLIC_IP))
                .isInstanceOf(GeolocationUnavailableException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"127.0.0.1", "10.0.0.1", "172.18.0.1", "192.168.1.10", "::1", "not an ip"})
    void returnsEmptyResultForPrivateOrInvalidAddressWithoutAskingProviders(String ipAddress) {
        FallbackGeolocationService service = service(
                address -> answer(primaryCalls, "PL"),
                address -> answer(secondaryCalls, "PL"));

        assertThat(service.findCountry(ipAddress)).isEmpty();
        assertThat(primaryCalls).hasValue(0);
        assertThat(secondaryCalls).hasValue(0);
    }

    private static FallbackGeolocationService service(GeolocationProvider primary, GeolocationProvider secondary) {
        return new FallbackGeolocationService(List.of(primary, secondary), 2, Duration.ofMillis(1));
    }

    private static Optional<String> answer(AtomicInteger calls, String country) {
        calls.incrementAndGet();
        return Optional.of(country);
    }

    private static Optional<String> serverError() {
        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
