package com.saasplatform.auth.service;

import com.saasplatform.auth.domain.User;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.elytron.security.common.BcryptUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class AuthServiceTest {

    @Test
    void passwordHash_shouldNotBeEqualToPlaintext() {
        String plain = "MySecret123";
        String hash = BcryptUtil.bcryptHash(plain);

        assertNotEquals(plain, hash);
        assertTrue(BcryptUtil.matches(plain, hash));
        assertFalse(BcryptUtil.matches("WrongPassword", hash));
    }

    @Test
    void passwordHash_shouldUseDifferentSaltsEachTime() {
        String plain = "MySecret123";
        String hash1 = BcryptUtil.bcryptHash(plain);
        String hash2 = BcryptUtil.bcryptHash(plain);

        // Two hashes of the same password must be different (different salts)
        assertNotEquals(hash1, hash2);
        // But both must validate correctly
        assertTrue(BcryptUtil.matches(plain, hash1));
        assertTrue(BcryptUtil.matches(plain, hash2));
    }
}
