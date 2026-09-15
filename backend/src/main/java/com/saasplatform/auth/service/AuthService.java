package com.saasplatform.auth.service;

import com.saasplatform.auth.domain.BusinessUser;
import com.saasplatform.auth.domain.RefreshToken;
import com.saasplatform.auth.domain.User;
import com.saasplatform.auth.domain.UserRole;
import com.saasplatform.auth.dto.AuthResponse;
import com.saasplatform.auth.dto.LoginRequest;
import com.saasplatform.auth.dto.RegisterRequest;
import com.saasplatform.business.domain.Business;
import com.saasplatform.business.domain.BusinessType;
import com.saasplatform.common.exception.BadRequestException;
import com.saasplatform.common.exception.ConflictException;
import com.saasplatform.common.exception.NotFoundException;
import io.quarkus.elytron.security.common.BcryptUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.jboss.logging.Logger;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Authentication service.
 * Handles registration (user + business creation), login, refresh, and logout.
 *
 * Multi-tenant note:
 * Registration creates a new Business (tenant) and makes the registering user its OWNER.
 * Future: support invitations to join an existing business.
 */
@ApplicationScoped
public class AuthService {

    private static final Logger LOG = Logger.getLogger(AuthService.class);
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9]+");

    @Inject
    JwtService jwtService;

    /**
     * Register a new user and create their Business (tenant).
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Check email uniqueness
        if (User.count("email = ?1", request.email()) > 0) {
            throw new ConflictException("An account with this email already exists");
        }

        // Create Business (type always defaults to OTHER — field removed from registration flow)
        Business business = new Business();
        business.name = request.businessName();
        business.slug = generateSlug(request.businessName());
        business.businessType = BusinessType.OTHER;
        business.persist();

        // Create User
        User user = new User();
        user.email = request.email().toLowerCase().trim();
        user.passwordHash = BcryptUtil.bcryptHash(request.password());
        user.firstName = request.firstName().trim();
        user.lastName = request.lastName().trim();
        user.emailVerified = false;
        user.persist();

        // Link user as OWNER of the business
        BusinessUser businessUser = new BusinessUser();
        businessUser.business = business;
        businessUser.user = user;
        businessUser.role = UserRole.OWNER;
        businessUser.persist();

        LOG.infof("Registered new user %s as OWNER of business %s (%s)",
                user.email, business.name, business.id);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user, business, UserRole.OWNER);
        String refreshToken = jwtService.generateRefreshToken(user, business.id);

        return buildAuthResponse(user, business, UserRole.OWNER, accessToken, refreshToken);
    }

    /**
     * Authenticate user with email/password.
     * Returns tokens for the user's primary (first active) business.
     * Future: support selecting which business to log into.
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = User.findByEmail(request.email().toLowerCase().trim());

        if (user == null || !BcryptUtil.matches(request.password(), user.passwordHash)) {
            // Same error message — don't reveal whether email exists
            throw new BadRequestException("Invalid email or password");
        }

        if (!user.active) {
            throw new BadRequestException("Account is disabled");
        }

        // Find first active business membership
        BusinessUser businessUser = BusinessUser.find(
                "user.id = ?1 AND active = true ORDER BY createdAt ASC", user.id
        ).firstResult();

        if (businessUser == null) {
            throw new NotFoundException("No active business found for this account");
        }

        Business business = Business.findById(businessUser.business.id);

        String accessToken = jwtService.generateAccessToken(user, business, businessUser.role);
        String refreshToken = jwtService.generateRefreshToken(user, business.id);

        LOG.infof("User %s logged in to business %s with role %s",
                user.email, business.name, businessUser.role);

        return buildAuthResponse(user, business, businessUser.role, accessToken, refreshToken);
    }

    /**
     * Refresh the access token using a valid refresh token.
     */
    @Transactional
    public AuthResponse refresh(String rawRefreshToken) {
        RefreshToken token = jwtService.validateRefreshToken(rawRefreshToken);

        if (token == null) {
            throw new BadRequestException("Invalid or expired refresh token");
        }

        User user = token.user;
        Business business = Business.findById(token.businessId);

        if (business == null || !business.active) {
            throw new BadRequestException("Business not found or inactive");
        }

        BusinessUser businessUser = BusinessUser.findByUserAndBusiness(user.id, business.id);
        if (businessUser == null) {
            throw new BadRequestException("User is no longer a member of this business");
        }

        // Rotate refresh token (revoke old, issue new)
        token.revoked = true;
        String newRefreshToken = jwtService.generateRefreshToken(user, business.id);
        String newAccessToken = jwtService.generateAccessToken(user, business, businessUser.role);

        return buildAuthResponse(user, business, businessUser.role, newAccessToken, newRefreshToken);
    }

    /**
     * Logout — revoke the given refresh token.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            jwtService.revokeRefreshToken(rawRefreshToken);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private AuthResponse buildAuthResponse(User user, Business business, UserRole role,
                                            String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                jwtService.getAccessExpirationSeconds(),
                new AuthResponse.UserDto(user.id, user.email, user.firstName, user.lastName, role.name()),
                new AuthResponse.BusinessDto(business.id, business.name, business.slug, business.businessType.name())
        );
    }

    private String generateSlug(String businessName) {
        // Normalize unicode, lowercase, remove non-alphanumeric
        String normalized = Normalizer.normalize(businessName, Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase(Locale.ROOT);
        String slug = NON_ALPHANUMERIC.matcher(normalized).replaceAll("-");
        // Remove leading/trailing hyphens
        slug = slug.replaceAll("^-+|-+$", "");

        // Ensure slug is unique
        String base = slug;
        int counter = 1;
        while (Business.slugExists(slug)) {
            slug = base + "-" + counter++;
        }
        return slug;
    }
}
