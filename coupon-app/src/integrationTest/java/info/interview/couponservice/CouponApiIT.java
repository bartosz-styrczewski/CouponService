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

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
class CouponApiIT {

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
    void createsCouponAndSavesItInDatabase() {
        createCoupon("""
                {"code": "spring", "maxUses": 100, "country": "pl"}
                """)
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.code").isEqualTo("SPRING")
                .jsonPath("$.country").isEqualTo("PL")
                .jsonPath("$.maxUses").isEqualTo(100)
                .jsonPath("$.currentUses").isEqualTo(0)
                .jsonPath("$.createdAt").isNotEmpty();

        assertThat(savedCoupons()).singleElement().satisfies(coupon -> {
            assertThat(coupon.code()).isEqualTo("SPRING");
            assertThat(coupon.country()).isEqualTo("PL");
            assertThat(coupon.maxUses()).isEqualTo(100);
            assertThat(coupon.currentUses()).isZero();
            assertThat(coupon.createdAt()).isNotNull();
        });
    }

    @Test
    void rejectsCodeThatDiffersOnlyInLetterCaseAndKeepsOriginalCoupon() {
        createCoupon("""
                {"code": "SUMMER", "maxUses": 10, "country": "PL"}
                """)
                .expectStatus().isCreated();

        createCoupon("""
                {"code": "summer", "maxUses": 99, "country": "DE"}
                """)
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("COUPON_ALREADY_EXISTS");

        assertThat(savedCoupons()).singleElement().satisfies(coupon -> {
            assertThat(coupon.code()).isEqualTo("SUMMER");
            assertThat(coupon.country()).isEqualTo("PL");
            assertThat(coupon.maxUses()).isEqualTo(10);
        });
    }

    @Test
    void rejectsInvalidCouponAndSavesNothing() {
        createCoupon("""
                {"code": "not valid!", "maxUses": 0, "country": "XX"}
                """)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errorCode").isEqualTo("VALIDATION_FAILED")
                .jsonPath("$.errors[*].field").value(new ParameterizedTypeReference<List<String>>() {
                }, fields -> assertThat(fields).containsExactlyInAnyOrder("code", "maxUses", "country"));

        assertThat(savedCoupons()).isEmpty();
    }

    @Test
    void rejectsMalformedJsonAndSavesNothing() {
        createCoupon("not json")
                .expectStatus().isBadRequest();

        assertThat(savedCoupons()).isEmpty();
    }

    private RestTestClient.ResponseSpec createCoupon(String json) {
        return restTestClient.post().uri("/api/coupons")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .exchange();
    }

    private List<CouponRow> savedCoupons() {
        return jdbcClient.sql("SELECT code, country, max_uses, current_uses, created_at FROM coupon")
                .query(CouponRow.class)
                .list();
    }

    private record CouponRow(String code, String country, int maxUses, int currentUses, OffsetDateTime createdAt) {
    }
}
