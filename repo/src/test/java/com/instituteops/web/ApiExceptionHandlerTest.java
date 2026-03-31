package com.instituteops.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void illegalArgument_returnsStructuredBadRequest() {
        MockHttpServletRequest request = request("/api/test");

        ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleIllegalArgument(
            new IllegalArgumentException("bad input"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("bad input");
        assertThat(response.getBody().path()).isEqualTo("/api/test");
        assertThat(response.getBody().traceId()).isEqualTo("n/a");
    }

    @Test
    void accessDenied_returnsStructuredForbidden() {
        MockHttpServletRequest request = request("/api/protected");

        ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAccessDenied(
            new org.springframework.security.access.AccessDeniedException("forbidden"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("forbidden");
    }

    @Test
    void authenticationFailure_returnsStructuredUnauthorized() {
        MockHttpServletRequest request = request("/api/secure");

        ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleAuthentication(
            new org.springframework.security.authentication.BadCredentialsException("bad creds"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Authentication required");
    }

    @Test
    void unexpectedException_hidesInternalMessageAndKeepsCorrelationId() {
        MockHttpServletRequest request = request("/api/test/unexpected");
        request.addHeader("X-Correlation-Id", "corr-123");

        ResponseEntity<ApiExceptionHandler.ApiErrorResponse> response = handler.handleUnexpected(
            new RuntimeException("secret details"),
            request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("Internal server error");
        assertThat(response.getBody().traceId()).isEqualTo("corr-123");
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI(path);
        return request;
    }
}
