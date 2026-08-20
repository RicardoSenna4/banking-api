package com.ricardosenna.bankingapi.security;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.ricardosenna.bankingapi.entity.UserEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final String ROLE_CLAIM = "role";

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.expiration-seconds}")
    private long expirationSeconds;

    @Value("${app.jwt.secret}")
    private String secretBase64;

    public String generateToken(UserEntity user) {
        try {
            Instant now = Instant.now();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(user.getUsername())
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(now.plusSeconds(expirationSeconds)))
                    .claim(ROLE_CLAIM, user.getRole().name())
                    .build();

            JWSHeader header = new JWSHeader(JWSAlgorithm.HS256);
            SignedJWT signedJWT = new SignedJWT(header, claims);

            JWSSigner signer = new MACSigner(getSecretKey());
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException("Error generating JWT token", e);
        }
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);

            JWSVerifier verifier = new MACVerifier(getSecretKey());
            boolean signatureValid = signedJWT.verify(verifier);

            if (!signatureValid) {
                log.warn("JWT signature validation failed");
                return false;
            }

            Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
            boolean notExpired = expirationTime != null && expirationTime.after(new Date());

            if (!notExpired) {
                log.warn("JWT token has expired");
            }

            return notExpired;
        } catch (ParseException | JOSEException e) {
            log.warn("JWT parsing or verification failed", e);
            return false;
        }
    }

    public String getSubject(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getSubject();
        } catch (ParseException e) {
            throw new RuntimeException("Error extracting subject from JWT", e);
        }
    }

    public String getRole(String token) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(token);
            return signedJWT.getJWTClaimsSet().getStringClaim(ROLE_CLAIM);
        } catch (ParseException e) {
            throw new RuntimeException("Error extracting role from JWT", e);
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] getSecretKey() {
        return Base64.getDecoder().decode(secretBase64);
    }
}
