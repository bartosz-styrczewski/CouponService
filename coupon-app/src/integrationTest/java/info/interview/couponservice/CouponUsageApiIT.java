package info.interview.couponservice;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import({TestcontainersConfiguration.class, GeolocationStubConfiguration.class})
class CouponUsageApiIT {

    private static final String POLISH_IP = "83.31.0.1";

    private static final String GERMAN_IP = "85.214.0.1";

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private WireMockServer geolocationStub;

    @Autowired
    private CacheManager cacheManager;

    @Value("${coupon.geolocation.retry.max-retries}")
    private int maxRetries;

    @BeforeEach
    void resetState() {
        jdbcClient.sql("DELETE FROM coupon_usage").update();
        jdbcClient.sql("DELETE FROM coupon").update();
        Objects.requireNonNull(cacheManager.getCache("countries")).clear();
        geolocationStub.resetAll();
        stubCountryIs(POLISH_IP, okJson("""
                {"ip": "83.31.0.1", "country": "PL"}
                """));
    }

    @Test
    void registersUsageAndIncrementsCurrentUses() {
        createCoupon("SUMMER", 2);

        registerUsage("summer", "user-1", POLISH_IP)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo("SUMMER")
                .jsonPath("$.userId").isEqualTo("user-1")
                .jsonPath("$.usedAt").isNotEmpty();

        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsSecondUsageBySameUser() {
        createCoupon("SUMMER", 5);
        registerUsage("SUMMER", "user-1", POLISH_IP).expectStatus().isCreated();

        registerUsage("SUMMER", "user-1", POLISH_IP)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_ALREADY_USED");

        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsUsageWhenCouponIsExhausted() {
        createCoupon("SUMMER", 1);
        registerUsage("SUMMER", "user-1", POLISH_IP).expectStatus().isCreated();

        registerUsage("SUMMER", "user-2", POLISH_IP)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_EXHAUSTED");

        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsUsageOfUnknownCoupon() {
        registerUsage("UNKNOWN", "user-1", POLISH_IP)
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_NOT_FOUND");

        assertThat(savedUsages()).isEmpty();
    }

    @Test
    void rejectsUsageWithoutUserIdAndSavesNothing() {
        createCoupon("SUMMER", 5);

        registerUsage("SUMMER", "", POLISH_IP)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors[*].field").value(new ParameterizedTypeReference<List<String>>() {
                }, fields -> assertThat(fields).containsExactly("userId"));

        assertThat(currentUses("SUMMER")).isZero();
        assertThat(savedUsages()).isEmpty();
    }

    @Test
    void rejectsUsageFromOtherCountryAndSavesNothing() {
        createCoupon("SUMMER", 5);
        stubCountryIs(GERMAN_IP, okJson("""
                {"ip": "85.214.0.1", "country": "DE"}
                """));

        registerUsage("SUMMER", "user-1", GERMAN_IP)
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUNTRY_NOT_ALLOWED");

        assertThat(currentUses("SUMMER")).isZero();
        assertThat(savedUsages()).isEmpty();
    }

    @Test
    void usesNextProviderWhenFirstOneFails() {
        createCoupon("SUMMER", 5);
        stubCountryIs(POLISH_IP, serverError());
        stubGeoJs(POLISH_IP, okJson("""
                {"country": "PL", "ip": "83.31.0.1", "name": "Poland"}
                """));

        registerUsage("SUMMER", "user-1", POLISH_IP).expectStatus().isCreated();

        geolocationStub.verify(1 + maxRetries, getRequestedFor(urlEqualTo("/country-is/" + POLISH_IP)));
        geolocationStub.verify(1, getRequestedFor(urlEqualTo("/geojs/v1/ip/country/" + POLISH_IP + ".json")));
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsUsageWhenGeolocationIsUnavailableAndSavesNothing() {
        createCoupon("SUMMER", 5);
        stubCountryIs(POLISH_IP, serverError());
        stubGeoJs(POLISH_IP, serverError());

        registerUsage("SUMMER", "user-1", POLISH_IP)
                .expectStatus().isEqualTo(HttpStatus.SERVICE_UNAVAILABLE)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("GEOLOCATION_UNAVAILABLE");

        assertThat(currentUses("SUMMER")).isZero();
        assertThat(savedUsages()).isEmpty();
    }

    @Test
    void asksGeolocationProviderOnlyOnceForSameIpAddress() {
        createCoupon("SUMMER", 5);

        registerUsage("SUMMER", "user-1", POLISH_IP).expectStatus().isCreated();
        registerUsage("SUMMER", "user-2", POLISH_IP).expectStatus().isCreated();

        geolocationStub.verify(1, getRequestedFor(urlEqualTo("/country-is/" + POLISH_IP)));
    }

    @Test
    void registersExactlyMaxUsesWhenManyUsersUseCouponAtOnce() throws Exception {
        createCoupon("SUMMER", 10);
        List<String> userIds = IntStream.range(0, 50).mapToObj(i -> "user-" + i).toList();

        List<Integer> statuses = registerUsagesAtOnce("SUMMER", userIds);

        assertThat(countByStatus(statuses)).containsExactlyInAnyOrderEntriesOf(Map.of(201, 10L, 409, 40L));
        assertThat(currentUses("SUMMER")).isEqualTo(10);
        assertThat(savedUsages()).hasSize(10);
    }

    @Test
    void registersOneUsageWhenSameUserUsesCouponManyTimesAtOnce() throws Exception {
        createCoupon("SUMMER", 100);

        List<Integer> statuses = registerUsagesAtOnce("SUMMER", Collections.nCopies(20, "user-1"));

        assertThat(countByStatus(statuses)).containsExactlyInAnyOrderEntriesOf(Map.of(201, 1L, 409, 19L));
        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    private List<Integer> registerUsagesAtOnce(String code, List<String> userIds) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(userIds.size())) {
            List<Future<Integer>> responses = userIds.stream()
                    .map(userId -> executor.submit(() -> {
                        start.await();
                        return registerUsage(code, userId, POLISH_IP).returnResult().getStatus().value();
                    }))
                    .toList();
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> response : responses) {
                statuses.add(response.get());
            }
            return statuses;
        }
    }

    private static Map<Integer, Long> countByStatus(List<Integer> statuses) {
        return statuses.stream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
    }

    private void stubCountryIs(String ipAddress, ResponseDefinitionBuilder response) {
        geolocationStub.stubFor(get(urlEqualTo("/country-is/" + ipAddress)).willReturn(response));
    }

    private void stubGeoJs(String ipAddress, ResponseDefinitionBuilder response) {
        geolocationStub.stubFor(get(urlEqualTo("/geojs/v1/ip/country/" + ipAddress + ".json")).willReturn(response));
    }

    private void createCoupon(String code, int maxUses) {
        restTestClient.post().uri("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"code": "%s", "maxUses": %d, "country": "PL"}
                        """.formatted(code, maxUses))
                .exchange()
                .expectStatus().isCreated();
    }

    private RestTestClient.ResponseSpec registerUsage(String code, String userId, String clientIp) {
        return restTestClient.post().uri("/api/coupons/{code}/usages", code)
                .header("X-Forwarded-For", clientIp)
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {"userId": "%s"}
                        """.formatted(userId))
                .exchange();
    }

    private int currentUses(String code) {
        return jdbcClient.sql("SELECT current_uses FROM coupon WHERE code = ?")
                .param(code)
                .query(Integer.class)
                .single();
    }

    private List<UsageRow> savedUsages() {
        return jdbcClient.sql("""
                        SELECT c.code, u.user_id
                        FROM coupon_usage u
                        JOIN coupon c ON c.id = u.coupon_id
                        """)
                .query(UsageRow.class)
                .list();
    }

    private record UsageRow(String code, String userId) {
    }
}
