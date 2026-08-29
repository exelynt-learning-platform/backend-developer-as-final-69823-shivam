package com.exelynt.booking.security;

import com.exelynt.booking.enums.Role;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtUtilTest {

    private static final String SECRET = "unit-test-signing-key-at-least-32-bytes-long";
    private static final String OTHER_SECRET = "a-completely-different-key-also-32-bytes-plus";

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, 3_600_000L);

    @Test
    void generatesTokenWithThreeSegments() {
        String token = jwtUtil.generateToken("alice", Role.USER);

        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void roundTripsUsername() {
        String token = jwtUtil.generateToken("alice", Role.USER);

        assertEquals("alice", jwtUtil.extractUsername(token));
    }

    @Test
    void acceptsTokenItSigned() {
        assertTrue(jwtUtil.isValid(jwtUtil.generateToken("admin", Role.ADMIN)));
    }

    @Test
    void rejectsTokenSignedWithAnotherKey() {
        JwtUtil attacker = new JwtUtil(OTHER_SECRET, 3_600_000L);
        String forged = attacker.generateToken("admin", Role.ADMIN);

        assertFalse(jwtUtil.isValid(forged));
    }

    @Test
    void rejectsTamperedPayload() {
        String token = jwtUtil.generateToken("alice", Role.USER);
        String[] parts = token.split("\\.");
        String tampered = parts[0] + "." + parts[1].substring(0, parts[1].length() - 2) + "XY." + parts[2];

        assertFalse(jwtUtil.isValid(tampered));
    }

    @Test
    void rejectsExpiredToken() {
        JwtUtil shortLived = new JwtUtil(SECRET, -1_000L);

        assertFalse(shortLived.isValid(shortLived.generateToken("alice", Role.USER)));
    }

    @Test
    void rejectsGarbage() {
        assertFalse(jwtUtil.isValid("not.a.jwt"));
        assertFalse(jwtUtil.isValid(""));
    }

    @Test
    void issuesDistinctTokensForDifferentUsers() {
        assertNotEquals(
                jwtUtil.generateToken("alice", Role.USER),
                jwtUtil.generateToken("bob", Role.USER));
    }
}
