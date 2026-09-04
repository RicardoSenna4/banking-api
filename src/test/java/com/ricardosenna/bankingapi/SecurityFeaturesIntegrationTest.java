package com.ricardosenna.bankingapi;

import com.ricardosenna.bankingapi.dto.*;
import com.ricardosenna.bankingapi.entity.UserEntity;
import com.ricardosenna.bankingapi.enums.AccountType;
import com.ricardosenna.bankingapi.exception.BusinessRuleException;
import com.ricardosenna.bankingapi.repository.UserRepository;
import com.ricardosenna.bankingapi.service.AuthService;
import com.ricardosenna.bankingapi.service.BankingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.annotation.DirtiesContext;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SecurityFeaturesIntegrationTest {
    @Autowired AuthService authService;
    @Autowired BankingService bankingService;
    @Autowired UserRepository userRepository;

    @Test
    void shouldRefreshAndRevokeToken() {
        authService.register(new RegisterRequest("Ricardo", "ricardo-refresh@example.com", "SenhaSegura123"));
        TokenResponse login = authService.login(new LoginRequest("ricardo-refresh@example.com", "SenhaSegura123"));
        assertNotNull(login.refreshToken());
        TokenResponse refreshed = authService.refresh(new RefreshTokenRequest(login.refreshToken()));
        assertNotNull(refreshed.accessToken());
        assertThrows(Exception.class, () -> authService.refresh(new RefreshTokenRequest(login.refreshToken())));
        authService.logout(new RefreshTokenRequest(refreshed.refreshToken()));
        assertThrows(Exception.class, () -> authService.refresh(new RefreshTokenRequest(refreshed.refreshToken())));
    }

    @Test
    void shouldRejectAccessToAnotherUsersAccount() {
        authService.register(new RegisterRequest("Ricardo", "ricardo-owner@example.com", "SenhaSegura123"));
        authService.register(new RegisterRequest("Joao", "joao-owner@example.com", "SenhaSegura123"));
        UserEntity ricardo = userRepository.findByEmailIgnoreCase("ricardo-owner@example.com").orElseThrow();
        UserEntity joao = userRepository.findByEmailIgnoreCase("joao-owner@example.com").orElseThrow();
        bankingService.createClient(joao, new ClientCreateRequest("98765432100", "Joao", "joao-client@example.com"));
        var account = bankingService.createAccount(joao, new AccountCreateRequest("98765432100", AccountType.CHECKING, BigDecimal.ZERO, null));
        assertThrows(AccessDeniedException.class, () -> bankingService.findByAccountNumber(ricardo, account.accountNumber()));
        assertThrows(AccessDeniedException.class, () -> bankingService.deposit(ricardo, new DepositRequest(account.accountNumber(), BigDecimal.TEN)));
    }
}
