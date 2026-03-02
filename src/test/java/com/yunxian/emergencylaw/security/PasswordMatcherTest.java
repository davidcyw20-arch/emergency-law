package com.yunxian.emergencylaw.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordMatcherTest {

    @Test
    void shouldMatchPlaintextPassword() {
        assertTrue(PasswordMatcher.matches("123456", "123456"));
    }

    @Test
    void shouldMatchMd5Password() {
        assertTrue(PasswordMatcher.matches("123456", "e10adc3949ba59abbe56e057f20f883e"));
    }

    @Test
    void shouldMatchPrefixedMd5Password() {
        assertTrue(PasswordMatcher.matches("123456", "{MD5}e10adc3949ba59abbe56e057f20f883e"));
    }

    @Test
    void shouldFailOnWrongPassword() {
        assertFalse(PasswordMatcher.matches("123456", "wrong-password"));
    }
}
