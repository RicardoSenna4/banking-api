package com.ricardosenna.bankingapi.service;

import com.ricardosenna.bankingapi.dto.LoginRequest;
import com.ricardosenna.bankingapi.dto.RegisterRequest;
import com.ricardosenna.bankingapi.dto.TokenResponse;
import com.ricardosenna.bankingapi.dto.UserResponse;
import com.ricardosenna.bankingapi.entity.UserEntity;
import com.ricardosenna.bankingapi.exception.DuplicateResourceException;
import com.ricardosenna.bankingapi.repository.UserRepository;
import com.ricardosenna.bankingapi.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new DuplicateResourceException("A user with this email already exists");
        }

        UserEntity user = new UserEntity(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                UserEntity.Role.USER
        );

        return UserResponse.from(userRepository.save(user));
    }

    public TokenResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password())
        );

        UserEntity user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RuntimeException("User not found after authentication"));

        String token = jwtService.generateToken(user);

        return new TokenResponse(token, jwtService.getExpirationSeconds());
    }
}
