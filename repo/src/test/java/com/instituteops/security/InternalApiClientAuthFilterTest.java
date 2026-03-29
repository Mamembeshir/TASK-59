package com.instituteops.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.instituteops.security.domain.InternalApiClientEntity;
import com.instituteops.security.repo.InternalApiClientRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InternalApiClientAuthFilterTest {

    @Mock
    private InternalApiClientRepository internalApiClientRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private InternalSyncPolicyService internalSyncPolicyService;

    private InternalApiClientAuthFilter filter;

    @BeforeEach
    void setUp() {
        filter = new InternalApiClientAuthFilter(internalApiClientRepository, passwordEncoder, internalSyncPolicyService);
    }

    @Test
    void deniesRequestWhenHeadersMissing() throws Exception {
        when(internalSyncPolicyService.currentPolicy()).thenReturn(new InternalSyncPolicyService.InternalSyncPolicy(true, false));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/ping");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {
            throw new AssertionError("Filter chain should not execute");
        });

        assertThat(response.getStatus()).isEqualTo(401);
        verify(internalApiClientRepository, never()).findByClientKeyAndActiveTrue(anyString());
    }

    @Test
    void authenticatesValidClientCredentials() throws Exception {
        when(internalSyncPolicyService.currentPolicy()).thenReturn(new InternalSyncPolicyService.InternalSyncPolicy(true, true));
        when(internalSyncPolicyService.isTrustedLanAddress(anyString())).thenReturn(true);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/internal/ping");
        request.setRemoteAddr("127.0.0.1");
        request.addHeader(InternalApiClientAuthFilter.API_KEY_HEADER, "local-sync-client");
        request.addHeader(InternalApiClientAuthFilter.API_SECRET_HEADER, "secret");
        MockHttpServletResponse response = new MockHttpServletResponse();

        InternalApiClientEntity client = org.mockito.Mockito.mock(InternalApiClientEntity.class);
        when(client.getClientKey()).thenReturn("local-sync-client");
        when(client.getClientSecretHash()).thenReturn("hashed");
        when(client.getExpiresAt()).thenReturn(LocalDateTime.now().plusDays(1));

        when(internalApiClientRepository.findByClientKeyAndActiveTrue("local-sync-client")).thenReturn(Optional.of(client));
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        filter.doFilter(request, response, (req, res) -> ((MockHttpServletResponse) res).setStatus(204));

        assertThat(response.getStatus()).isEqualTo(204);
        verify(internalApiClientRepository).findByClientKeyAndActiveTrue("local-sync-client");
        verify(internalSyncPolicyService).isTrustedLanAddress(eq("127.0.0.1"));
    }
}
