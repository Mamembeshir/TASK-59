package com.instituteops.security;

import com.instituteops.security.domain.InternalApiClientEntity;
import com.instituteops.security.repo.InternalApiClientRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class InternalApiClientAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-API-KEY";
    public static final String API_SECRET_HEADER = "X-API-SECRET";

    private final InternalApiClientRepository internalApiClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final InternalSyncPolicyService internalSyncPolicyService;

    public InternalApiClientAuthFilter(
        InternalApiClientRepository internalApiClientRepository,
        PasswordEncoder passwordEncoder,
        InternalSyncPolicyService internalSyncPolicyService
    ) {
        this.internalApiClientRepository = internalApiClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.internalSyncPolicyService = internalSyncPolicyService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !(path.equals("/api/internal") || path.startsWith("/api/internal/"));
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        InternalSyncPolicyService.InternalSyncPolicy policy = internalSyncPolicyService.currentPolicy();
        if (!policy.enabled()) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Internal sync API is disabled");
            return;
        }
        if (policy.lanOnly() && !internalSyncPolicyService.isTrustedLanAddress(request.getRemoteAddr())) {
            response.sendError(HttpStatus.FORBIDDEN.value(), "Internal sync API is restricted to trusted LAN addresses");
            return;
        }

        String key = request.getHeader(API_KEY_HEADER);
        String secret = request.getHeader(API_SECRET_HEADER);

        if (key == null || secret == null) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Missing API key or secret");
            return;
        }

        InternalApiClientEntity client = internalApiClientRepository.findByClientKeyAndActiveTrue(key)
            .filter(c -> c.getExpiresAt() == null || c.getExpiresAt().isAfter(LocalDateTime.now()))
            .orElse(null);

        if (client == null || !passwordEncoder.matches(secret, client.getClientSecretHash())) {
            response.sendError(HttpStatus.UNAUTHORIZED.value(), "Invalid API credentials");
            return;
        }

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "api:" + client.getClientKey(),
            null,
            List.of(new SimpleGrantedAuthority("API_INTERNAL"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
