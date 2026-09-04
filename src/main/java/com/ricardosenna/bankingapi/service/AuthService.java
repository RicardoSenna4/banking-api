package com.ricardosenna.bankingapi.service;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.entity.UserEntity;
import com.ricardosenna.bankingapi.exception.*;
import com.ricardosenna.bankingapi.repository.UserRepository;
import com.ricardosenna.bankingapi.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository; private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager; private final JwtService jwtService;
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager, JwtService jwtService) {
        this.userRepository = userRepository; this.passwordEncoder = passwordEncoder; this.authenticationManager = authenticationManager; this.jwtService = jwtService;
    }
    @Transactional public UserResponse register(RegisterRequest request) {
        String email = request.email().toLowerCase().trim();
        if (userRepository.existsByEmailIgnoreCase(email)) throw new DuplicateResourceException("A user with this email already exists");
        return UserResponse.from(userRepository.save(new UserEntity(request.name().trim(), email, passwordEncoder.encode(request.password()), UserEntity.Role.USER)));
    }
    public TokenResponse login(LoginRequest request) {
        String email = request.email().toLowerCase().trim();
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        UserEntity user = findByEmail(email); return new TokenResponse(jwtService.generateToken(user), jwtService.getExpirationSeconds());
    }
    @Transactional public UserResponse updateCurrentUser(String currentEmail, UserUpdateRequest request) {
        UserEntity user = findByEmail(currentEmail);
        String email = request.email().toLowerCase().trim();
        userRepository.findByEmailIgnoreCase(email).filter(other -> !other.getId().equals(user.getId())).ifPresent(other -> { throw new DuplicateResourceException("A user with this email already exists"); });
        user.updateProfile(request.name().trim(), email); return UserResponse.from(userRepository.save(user));
    }
    @Transactional public void changePassword(String currentEmail, ChangePasswordRequest request) {
        UserEntity user = findByEmail(currentEmail);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) throw new BadCredentialsException("Current password is incorrect");
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) throw new BusinessRuleException("New password must be different from current password");
        user.updatePassword(passwordEncoder.encode(request.newPassword())); userRepository.save(user);
    }
    private UserEntity findByEmail(String email) { return userRepository.findByEmailIgnoreCase(email).orElseThrow(() -> new ResourceNotFoundException("User not found")); }
}
