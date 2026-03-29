package com.instituteops.audit;

import com.instituteops.security.UserIdentityService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestAuditFilter extends OncePerRequestFilter {

    private final AuditLogService auditLogService;
    private final UserIdentityService userIdentityService;

    public RequestAuditFilter(AuditLogService auditLogService, UserIdentityService userIdentityService) {
        this.auditLogService = auditLogService;
        this.userIdentityService = userIdentityService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/") || path.startsWith("/js/") || path.startsWith("/images/") || path.equals("/favicon.ico");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        String requestId = Optional.ofNullable(request.getHeader("X-Request-ID"))
            .filter(v -> !v.isBlank())
            .orElse(UUID.randomUUID().toString());
        request.setAttribute("requestId", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String actorUsername = authentication == null ? "anonymous" : authentication.getName();
            String roles = authentication == null
                ? ""
                : authentication.getAuthorities().stream().map(a -> a.getAuthority()).sorted().collect(Collectors.joining(","));

            Long actorUserId = userIdentityService.resolveCurrentUserId().orElse(null);
            boolean success = response.getStatus() < 400;
            String path = request.getRequestURI();
            String action = request.getMethod() + " " + path;
            String entityType = inferEntityType(path);
            Long entityId = inferEntityId(path);

            auditLogService.logOperation(
                actorUserId,
                actorUsername,
                roles,
                action,
                entityType,
                entityId,
                requestId,
                request.getRemoteAddr(),
                success,
                "HTTP " + response.getStatus()
            );

            if ("STUDENT".equals(entityType) && "GET".equalsIgnoreCase(request.getMethod())) {
                String accessType = hasUnmaskPrivilege(authentication) && "true".equalsIgnoreCase(request.getParameter("unmask"))
                    ? "UNMASKED_READ"
                    : "MASKED_READ";
                auditLogService.logDataAccess(actorUserId, entityType, entityId == null ? 0L : entityId, accessType, action, requestId);
            }
        }
    }

    private static boolean hasUnmaskPrivilege(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .anyMatch(a -> "ROLE_SYSTEM_ADMIN".equals(a.getAuthority()) || "ROLE_REGISTRAR_FINANCE_CLERK".equals(a.getAuthority()));
    }

    private static String inferEntityType(String path) {
        if (path.startsWith("/student") || path.startsWith("/api/students")) {
            return "STUDENT";
        }
        if (path.startsWith("/inventory")) {
            return "INVENTORY";
        }
        if (path.startsWith("/procurement")) {
            return "PROCUREMENT";
        }
        if (path.startsWith("/store")) {
            return "GROUP_BUY";
        }
        return null;
    }

    private static Long inferEntityId(String path) {
        String[] segments = path.split("/");
        if (segments.length == 0) {
            return null;
        }
        String candidate = segments[segments.length - 1];
        try {
            return Long.parseLong(candidate);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
