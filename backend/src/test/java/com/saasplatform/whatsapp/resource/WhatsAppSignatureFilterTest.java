package com.saasplatform.whatsapp.resource;

import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class WhatsAppSignatureFilterTest {

    @Test
    void hmacSha256_shouldProduceDeterministicSignature() throws Exception {
        String secret = "test_app_secret";
        String payload = "{\"object\":\"whatsapp_business_account\"}";

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig1 = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        // Reset and compute again
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String sig2 = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        assertEquals(sig1, sig2, "HMAC must be deterministic");
        assertEquals(64, sig1.length(), "SHA-256 hex must be 64 chars");
    }

    @Test
    void timingSafeEquals_shouldRejectDifferentLengths() {
        assertFalse(timingSafeEquals("sha256=abc", "sha256=abcd"));
    }

    @Test
    void timingSafeEquals_shouldAcceptEqualStrings() {
        assertTrue(timingSafeEquals("sha256=abc123", "sha256=abc123"));
    }

    private boolean timingSafeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
