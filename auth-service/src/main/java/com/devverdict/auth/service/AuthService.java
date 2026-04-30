package com.devverdict.auth.service;

import com.devverdict.auth.domain.Role;
import com.devverdict.auth.domain.User;
import com.devverdict.auth.dto.AuthResponse;
import com.devverdict.auth.dto.LoginRequest;
import com.devverdict.auth.dto.RegisterRequest;
import com.devverdict.auth.repository.UserRepository;
import com.devverdict.auth.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider,
                       AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authenticationManager = authenticationManager;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already registered");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new IllegalArgumentException("Username already taken");
        }

        String passwordHash = passwordEncoder.encode(request.password());
        User user = new User(request.email(), request.username(), passwordHash, Role.USER);
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        if (!authentication.isAuthenticated()) {
            throw new BadCredentialsException("Invalid credentials");
        }

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, "Bearer", user.getId(), user.getUsername(), user.getEmail(), user.getRole().name());
    }
}
