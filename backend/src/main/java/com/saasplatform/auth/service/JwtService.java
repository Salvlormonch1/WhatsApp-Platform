package com.saasplatform.auth.service;

import com.saasplatform.auth.domain.RefreshToken;
import com.saasplatform.auth.domain.User;
import com.saasplatform.auth.domain.UserRole;
import com.saasplatform.business.domain.Business;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;

/**
 * JWT token generation and refresh token management service.
 *
 * Access tokens: short-lived JWTs (15 min default), contain userId, businessId, role.
 * Refresh tokens: long-lived random tokens stored as SHA-256 hash in DB.
 *
 * Security notes:
 * - Only the hash of the refresh token is persisted.
 * - The raw token is only returned once during login/refresh.
 * - businessId in JWT is the source of truth for tenant isolation.
 */
@ApplicationScoped
public class JwtService {

    private static final Logger LOG = Logger.getLogger(JwtService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @ConfigProperty(name = "platform.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "platform.jwt.access.expiration.minutes", defaultValue = "15")
    long accessExpirationMinutes;

    @ConfigProperty(name = "platform.jwt.refresh.expiration.days", defaultValue = "7")
    long refreshExpirationDays;

    /**
     * Generate a signed JWT access token for the given user/business/role.
     */
    public String generateAccessToken(User user, Business business, UserRole role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(Duration.ofMinutes(accessExpirationMinutes));

        return Jwt.issuer(issuer)
                .subject(user.id.toString())
                // 'groups' is required by MicroProfile JWT spec for @RolesAllowed
                .groups(Set.of(role.name()))
                .claim("email", user.email)
                .claim("firstName", user.firstName)
                .claim("lastName", user.lastName)
                .claim("businessId", business.id.toString())
                .claim("businessName", business.name)
                .claim("businessSlug", business.slug)
                // Keep 'role' for frontend readability
                .claim("role", role.name())
                .issuedAt(now)
                .expiresAt(expiry)
                .sign();
    }

    /**
     * Generate a cryptographically secure refresh token and persist its hash.
     */
    @Transactional
    public String generateRefreshToken(User user, UUID businessId) {
        // Generate 32 random bytes → base64url
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // Hash the token before storing
        String tokenHash = sha256(rawToken);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.user = user;
        refreshToken.businessId = businessId;
        refreshToken.tokenHash = tokenHash;
        refreshToken.expiresAt = Instant.now().plus(Duration.ofDays(refreshExpirationDays));
        refreshToken.persist();

        return rawToken;
    }

    /**
     * Validate a refresh token and return the persisted entity if valid.
     */
    public RefreshToken validateRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        RefreshToken token = RefreshToken.findByTokenHash(tokenHash);

        if (token == null) {
            LOG.warn("Refresh token not found");
            return null;
        }

        if (!token.isValid()) {
            LOG.warnf("Refresh token invalid/expired for user %s", token.user.id);
            return null;
        }

        return token;
    }

    /**
     * Revoke a specific refresh token.
     */
    @Transactional
    public void revokeRefreshToken(String rawToken) {
        String tokenHash = sha256(rawToken);
        RefreshToken token = RefreshToken.findByTokenHash(tokenHash);
        if (token != null) {
            token.revoked = true;
        }
    }

    /**
     * Revoke all refresh tokens for a user (e.g., password change, security event).
     */
    @Transactional
    public void revokeAllUserTokens(UUID userId) {
        RefreshToken.revokeAllForUser(userId);
    }

    public long getAccessExpirationSeconds() {
        return accessExpirationMinutes * 60;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
