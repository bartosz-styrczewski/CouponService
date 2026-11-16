package info.interview.couponservice.geolocation.impl;

import info.interview.couponservice.geolocation.GeolocationService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;

@Configuration(proxyBeanMethods = false)
@EnableCaching
@EnableConfigurationProperties(GeolocationProperties.class)
class GeolocationConfiguration {

    private static final Map<String, Function<RestClient, GeolocationProvider>> PROVIDERS = Map.of(
            CountryIsProvider.NAME, CountryIsProvider::new,
            GeoJsProvider.NAME, GeoJsProvider::new);

    @Bean
    GeolocationService geolocationService(GeolocationProperties properties, RestClient.Builder restClientBuilder) {
        return new FallbackGeolocationService(
                providers(properties, restClientBuilder),
                properties.retry().maxRetries(),
                properties.retry().delay());
    }

    static List<GeolocationProvider> providers(GeolocationProperties properties, RestClient.Builder restClientBuilder) {
        if (properties.providers().isEmpty()) {
            throw new IllegalStateException(
                    "No geolocation provider set in coupon.geolocation.providers, available: " + availableProviders());
        }
        ClientHttpRequestFactory requestFactory = requestFactory(properties);
        return properties.providers().stream()
                .map(name -> provider(name, properties, restClientBuilder, requestFactory))
                .toList();
    }

    private static GeolocationProvider provider(
            String name,
            GeolocationProperties properties,
            RestClient.Builder restClientBuilder,
            ClientHttpRequestFactory requestFactory) {
        Function<RestClient, GeolocationProvider> factory = PROVIDERS.get(name);
        if (factory == null) {
            throw new IllegalStateException(
                    "Unknown geolocation provider '%s', available: %s".formatted(name, availableProviders()));
        }
        GeolocationProperties.Provider settings = properties.provider().get(name);
        if (settings == null || settings.url() == null || settings.url().isBlank()) {
            throw new IllegalStateException("Missing coupon.geolocation.provider.%s.url".formatted(name));
        }
        return factory.apply(restClientBuilder.clone()
                .requestFactory(requestFactory)
                .baseUrl(settings.url())
                .build());
    }

    private static String availableProviders() {
        return String.join(", ", new TreeSet<>(PROVIDERS.keySet()));
    }

    private static ClientHttpRequestFactory requestFactory(GeolocationProperties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return requestFactory;
    }
}
