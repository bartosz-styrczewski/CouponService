package info.interview.couponservice.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void unexpectedErrorReturnsInternalServerErrorWithoutExceptionMessage() {
        ProblemDetail problemDetail = handler.handleUnexpectedError(
                new IllegalStateException("connection to database refused"),
                new MockHttpServletRequest("POST", "/api/coupons"));

        assertThat(problemDetail.getStatus()).isEqualTo(500);
        assertThat(problemDetail.getProperties()).containsEntry("errorCode", "INTERNAL_ERROR");
        assertThat(problemDetail.getDetail()).doesNotContain("connection to database refused");
    }
}
