package com.devverdict.review.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey key;

    public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
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

                wrappedRequest.addHeader("X-User-Id", tokenUserId);
                wrappedRequest.addHeader("X-User-Role", role != null ? role : "USER");
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid token — continue without adding headers
            }
        }

        filterChain.doFilter(wrappedRequest, response);
    }
}
