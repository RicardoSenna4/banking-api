package com.ricardosenna.bankingapi.service;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.entity.*;
import com.ricardosenna.bankingapi.exception.*;
import com.ricardosenna.bankingapi.repository.*;
import com.ricardosenna.bankingapi.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class AuthService {
    private final UserRepository userRepository; private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder; private final AuthenticationManager authenticationManager; private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();
    @Value("${app.jwt.refresh-expiration-seconds:604800}") private long refreshExpirationSeconds;
    public AuthService(UserRepository userRepository, RefreshTokenRepository refreshTokenRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository; this.refreshTokenRepository = refreshTokenRepository; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }
    @Transactional public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("A user with this email already exists");
        return UserResponse.from(userRepository.save(new UserEntity(request.name().trim(), email, passwordEncoder.encode(request.password()), UserEntity.Role.USER)));
    }
    @Transactional public TokenResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        UserEntity user = findByEmail(email);
        return issueTokens(user);
    }
    @Transactional public TokenResponse refresh(RefreshTokenRequest request) {
        RefreshTokenEntity stored = refreshTokenRepository.findByTokenHash(hash(request.refreshToken())).filter(RefreshTokenEntity::isValid).orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));
        stored.revoke(); refreshTokenRepository.save(stored);
        return issueTokens(stored.getUser());
    }
    @Transactional public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByTokenHash(hash(request.refreshToken())).ifPresent(token -> { token.revoke(); refreshTokenRepository.save(token); });
    }
    @Transactional public UserResponse updateCurrentUser(String currentEmail, UserUpdateRequest request) {
        UserEntity user = findByEmail(currentEmail); String email = request.email().toLowerCase().trim();
        userRepository.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(user.getId())).ifPresent(other -> { throw new DuplicateResourceException("A user with this email already exists"); });
        user.updateProfile(request.name().trim(), email); return UserResponse.from(userRepository.save(user));
    }
    @Transactional public void changePassword(String currentEmail, ChangePasswordRequest request) {
        UserEntity user = findByEmail(currentEmail);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) throw new BadCredentialsException("Current password is incorrect");
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) throw new BusinessRuleException("New password must be different from current password");
        user.updatePassword(passwordEncoder.encode(request.newPassword())); userRepository.save(user); refreshTokenRepository.deleteAllByUserId(user.getId());
    }
    private TokenResponse issueTokens(UserEntity user) {
        byte[] bytes = new byte[48]; secureRandom.nextBytes(bytes); String refresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        refreshTokenRepository.save(new RefreshTokenEntity(user, hash(refresh), Instant.now().plusSeconds(refreshExpirationSeconds)));
        return new TokenResponse(jwtService.generateToken(user), refresh, jwtService.getExpirationSeconds());
    }
    private UserEntity findByEmail(String email) { return userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
    private String hash(String value) { try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception e) { throw new IllegalStateException(e); } }
}
