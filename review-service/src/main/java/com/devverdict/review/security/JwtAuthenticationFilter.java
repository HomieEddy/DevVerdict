package com.devverdict.review.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private final SecretKey key;

    public JwtAuthenticationFilter(String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        logger.debug("JwtAuthenticationFilter processing request to {} - Authorization header present: {}",
                request.getRequestURI(), authHeader != null);

        HeaderMapRequestWrapper wrappedRequest = new HeaderMapRequestWrapper(request);

        // Strip any incoming internal headers to prevent header forgery
        wrappedRequest.removeHeader("X-User-Id");
        wrappedRequest.removeHeader("X-User-Role");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                Claims claims = Jwts.parser()
                        .verifyWith(key)
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                String tokenUserId = claims.getSubject();
                String role = claims.get("role", String.class);
                logger.debug("JWT validated successfully - userId: {}, role: {}", tokenUserId, role);

                wrappedRequest.addHeader("X-User-Id", tokenUserId);
                wrappedRequest.addHeader("X-User-Role", role != null ? role : "USER");

                // Populate Spring Security context for filter-chain authorization
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        tokenUserId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
                logger.debug("Spring Security context populated for userId: {}", tokenUserId);
            } catch (JwtException | IllegalArgumentException e) {
                logger.warn("JWT validation failed: {}", e.getMessage());
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}
