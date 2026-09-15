package com.saasplatform.whatsapp.resource;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Validates the X-Hub-Signature-256 header on WhatsApp webhook POST requests.
 * Rejects any request that doesn't come from Meta.
 *
 * Meta signs the payload with HMAC-SHA256 using the app secret:
 *   sha256=<hex(HMAC-SHA256(app_secret, raw_body))>
 */
@Provider
@jakarta.annotation.Priority(1)
public class WhatsAppSignatureFilter implements ContainerRequestFilter {

    private static final Logger LOG = Logger.getLogger(WhatsAppSignatureFilter.class);
    private static final String SIGNATURE_HEADER = "X-Hub-Signature-256";
    private static final String WEBHOOK_PATH = "/api/v1/whatsapp/webhook";

    @ConfigProperty(name = "platform.whatsapp.app-secret", defaultValue = "")
    String appSecret;

    @ConfigProperty(name = "platform.whatsapp.signature-validation.enabled", defaultValue = "true")
    boolean validationEnabled;

    @Override
    public void filter(ContainerRequestContext ctx) throws IOException {
        // Only validate POST to webhook path
        if (!"POST".equals(ctx.getMethod()) ||
            !ctx.getUriInfo().getPath().startsWith(WEBHOOK_PATH.substring(1))) {
            return;
        }

        if (!validationEnabled || appSecret == null || appSecret.isBlank()) {
            if (appSecret == null || appSecret.isBlank()) {
                LOG.warn("WHATSAPP_APP_SECRET not configured — signature validation SKIPPED. Set it in production!");
            } else {
                LOG.warn("WhatsApp signature validation is DISABLED — enable in production!");
            }
            return;
        }

        String signatureHeader = ctx.getHeaderString(SIGNATURE_HEADER);
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            LOG.warn("Missing or malformed X-Hub-Signature-256 header");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Missing signature\"}").build());
            return;
        }

        // Buffer the body (we need to read it twice)
        byte[] body = ctx.getEntityStream().readAllBytes();
        ctx.setEntityStream(new ByteArrayInputStream(body));

        String expectedSignature = "sha256=" + computeHmacSha256(appSecret, body);
        if (!timingSafeEquals(signatureHeader, expectedSignature)) {
            LOG.warnf("WhatsApp signature mismatch — possible spoofed request");
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\":\"Invalid signature\"}").build());
        }
    }

    private String computeHmacSha256(String secret, byte[] payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("HMAC-SHA256 computation failed", e);
        }
    }

    /** Constant-time comparison to prevent timing attacks */
    private boolean timingSafeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
