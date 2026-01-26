package com.vanshpal.ShareFile.filters;

import com.vanshpal.ShareFile.service.entityClasses.SessionAuth;
import com.vanshpal.ShareFile.service.sessionService.helperClasses.Role;
import com.vanshpal.ShareFile.service.sessionService.repositories.SessionAuthRepository;
import com.vanshpal.ShareFile.service.sessionService.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class TokenFilter extends OncePerRequestFilter {
    private final SessionAuthRepository authRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractToken(request);
        String sessionId = request.getHeader("SESSION-ID");
        if (token == null || sessionId == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing token");
            return;
        }
        try {
            validateToken(sessionId, token, request);
        } catch (RuntimeException e) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }

    private void validateToken(String sessionId, String token, HttpServletRequest request) {

        String tokenHash = SessionService.hash(token);

        Role role = request.getRequestURI().contains("upload")? Role.SENDER: Role.RECEIVER;

        SessionAuth auth = authRepository
                .findBySessionIdAndRole(sessionId, role)
                .orElseThrow(() -> new RuntimeException("Auth not found"));

        if (auth.isRevoked())
            throw new RuntimeException("Token revoked");

        if (LocalDateTime.now().isAfter(auth.getExpiresAt()))
            throw new RuntimeException("Token expired");

        if (!auth.getTokenHash().equals(tokenHash))
            throw new RuntimeException("Invalid token");
    }
}
