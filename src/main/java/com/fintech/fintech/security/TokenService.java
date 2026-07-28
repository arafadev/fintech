package com.fintech.fintech.security;

import com.fintech.fintech.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenBlacklistService tokenBlacklistService;

    @Value("${jwt.secret.string}")
    private String JWT_SECRETE;

    @Value("${jwt.expiration.time}")
    private long EXPIRATION_TIME;

    private SecretKey key;

    @PostConstruct
    private void init() {
        byte[] keyByte = JWT_SECRETE.getBytes(StandardCharsets.UTF_8);
        this.key = new SecretKeySpec(keyByte, "HmacSHA256");
    }


    public String generateToken(User user) {
        Long version = user.getTokenVersion() == null ? 0L : user.getTokenVersion();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("tv", version)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return extractClaims(token, Claims::getSubject);
    }

    public Date getExpirationFromToken(String token) {
        return extractClaims(token, Claims::getExpiration);
    }

    private <T> T extractClaims(String token, Function<Claims, T> claimsTFunction) {
        return claimsTFunction.apply(
                Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload()
        );
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = getUsernameFromToken(token);

        if (!userDetails.isEnabled()) {
            log.warn("Rejected token for disabled user: {}", username);
            return false;
        }

        if (tokenBlacklistService.isBlacklisted(token)) {
            log.warn("Rejected blacklisted token for user: {}", username);
            return false;
        }

        if (userDetails instanceof AuthUser authUser) {
            Long tokenVersion = extractClaims(token, claims -> claims.get("tv", Long.class));
            Long currentVersion = authUser.getUser().getTokenVersion();
            if (currentVersion == null) {
                currentVersion = 0L;
            }
            if (tokenVersion == null || !tokenVersion.equals(currentVersion)) {
                log.warn("Rejected token due to tokenVersion mismatch for user: {}", username);
                return false;
            }
        }

        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractClaims(token, Claims::getExpiration).before(new Date());
    }

    public void revokeToken(String token) {
        Date expiration = getExpirationFromToken(token);
        tokenBlacklistService.blacklist(token, expiration.getTime());
    }
}
