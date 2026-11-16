package info.interview.couponservice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class CouponUsageApiIT {

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcClient jdbcClient;

    @BeforeEach
    void deleteCoupons() {
        jdbcClient.sql("DELETE FROM coupon_usage").update();
        jdbcClient.sql("DELETE FROM coupon").update();
    }

    @Test
    void registersUsageAndIncrementsCurrentUses() {
        createCoupon("SUMMER", 2);

        registerUsage("summer", "user-1")
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
        registerUsage("SUMMER", "user-1").expectStatus().isCreated();

        registerUsage("SUMMER", "user-1")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_ALREADY_USED");

        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsUsageWhenCouponIsExhausted() {
        createCoupon("SUMMER", 1);
        registerUsage("SUMMER", "user-1").expectStatus().isCreated();

        registerUsage("SUMMER", "user-2")
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_EXHAUSTED");

        assertThat(currentUses("SUMMER")).isEqualTo(1);
        assertThat(savedUsages()).containsExactly(new UsageRow("SUMMER", "user-1"));
    }

    @Test
    void rejectsUsageOfUnknownCoupon() {
        registerUsage("UNKNOWN", "user-1")
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_NOT_FOUND");

        assertThat(savedUsages()).isEmpty();
    }

    @Test
    void rejectsUsageWithoutUserIdAndSavesNothing() {
        createCoupon("SUMMER", 5);

        registerUsage("SUMMER", "")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors[*].field").value(new ParameterizedTypeReference<List<String>>() {
                }, fields -> assertThat(fields).containsExactly("userId"));

        assertThat(currentUses("SUMMER")).isZero();
        assertThat(savedUsages()).isEmpty();
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

    private RestTestClient.ResponseSpec registerUsage(String code, String userId) {
        return restTestClient.post().uri("/api/coupons/{code}/usages", code)
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
