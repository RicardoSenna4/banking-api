package com.ricardosenna.bankingapi;

import com.ricardosenna.bankingapi.entity.UserEntity;
import com.ricardosenna.bankingapi.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class JwtServiceTest {

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldGenerateAndValidateToken() {
        UserEntity user = new UserEntity("Test User", "test@example.com", "encodedPassword", UserEntity.Role.USER);
        user.setId(1L);

        String token = jwtService.generateToken(user);

        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("test@example.com", jwtService.getSubject(token));
        assertEquals("USER", jwtService.getRole(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.here"));
    }

    @Test
    void shouldRejectTamperedToken() {
        UserEntity user = new UserEntity("Test User", "test@example.com", "encodedPassword", UserEntity.Role.USER);
        user.setId(1L);

        String token = jwtService.generateToken(user);
        String tampered = token + "tampered";

        assertFalse(jwtService.validateToken(tampered));
    }
}
