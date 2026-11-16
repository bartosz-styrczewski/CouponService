package info.interview.couponservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Replaces both geolocation providers with a local WireMock server, so tests never call the real services.
 */
@TestConfiguration(proxyBeanMethods = false)
class GeolocationStubConfiguration {

    @Bean(destroyMethod = "stop")
    WireMockServer geolocationStub() {
        WireMockServer server = new WireMockServer(options().dynamicPort());
        server.start();
        return server;
    }

    @Bean
    DynamicPropertyRegistrar geolocationProperties(WireMockServer geolocationStub) {
        return registry -> {
            registry.add("coupon.geolocation.provider.country-is.url", () -> geolocationStub.baseUrl() + "/country-is");
            registry.add("coupon.geolocation.provider.geojs.url", () -> geolocationStub.baseUrl() + "/geojs");
        };
    }
}
