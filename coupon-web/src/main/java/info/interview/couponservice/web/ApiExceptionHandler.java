package info.interview.couponservice.web;

import info.interview.couponservice.service.CouponAlreadyExistsException;
import info.interview.couponservice.service.CouponAlreadyUsedException;
import info.interview.couponservice.service.CouponExhaustedException;
import info.interview.couponservice.service.CouponNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler
    ProblemDetail handleCouponAlreadyExists(CouponAlreadyExistsException exception, HttpServletRequest request) {
        return rejectRequest(request, HttpStatus.CONFLICT, "COUPON_ALREADY_EXISTS", exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handleCouponNotFound(CouponNotFoundException exception, HttpServletRequest request) {
        return rejectRequest(request, HttpStatus.NOT_FOUND, "COUPON_NOT_FOUND", exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handleCouponAlreadyUsed(CouponAlreadyUsedException exception, HttpServletRequest request) {
        return rejectRequest(request, HttpStatus.CONFLICT, "COUPON_ALREADY_USED", exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handleCouponExhausted(CouponExhaustedException exception, HttpServletRequest request) {
        return rejectRequest(request, HttpStatus.CONFLICT, "COUPON_EXHAUSTED", exception.getMessage());
    }

    @ExceptionHandler
    ProblemDetail handleUnexpectedError(Exception exception, HttpServletRequest request) {
        log.error("{} failed with an unexpected error", describe(request), exception);
        return problemDetail(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected error");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problemDetail = exception.getBody();
        problemDetail.setProperty("errorCode", "VALIDATION_FAILED");
        problemDetail.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(), error.getDefaultMessage()))
                .toList());
        return handleExceptionInternal(exception, problemDetail, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception exception, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (statusCode.is5xxServerError()) {
            log.error("{} failed", describe(request), exception);
        } else if (body instanceof ProblemDetail problemDetail) {
            logRejectedRequest(describe(request), problemDetail);
        }
        return super.handleExceptionInternal(exception, body, headers, statusCode, request);
    }

    private static ProblemDetail rejectRequest(
            HttpServletRequest request, HttpStatus status, String errorCode, String detail) {
        ProblemDetail problemDetail = problemDetail(status, errorCode, detail);
        logRejectedRequest(describe(request), problemDetail);
        return problemDetail;
    }

    private static void logRejectedRequest(String request, ProblemDetail problemDetail) {
        Map<String, Object> properties = Objects.requireNonNullElse(problemDetail.getProperties(), Map.of());
        log.warn("{} rejected with status {}: {} {}",
                request, problemDetail.getStatus(), problemDetail.getDetail(), properties);
    }

    private static ProblemDetail problemDetail(HttpStatus status, String errorCode, String detail) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("errorCode", errorCode);
        return problemDetail;
    }

    private static String describe(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? describe(servletRequest.getRequest())
                : request.getDescription(false);
    }

    private static String describe(HttpServletRequest request) {
        return request.getMethod() + " " + request.getRequestURI();
    }

    record FieldViolation(String field, String message) {
    }
}
