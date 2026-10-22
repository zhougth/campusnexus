package com.campusnexus.security;

import java.time.Instant;

public record JwtPayload(
        Long userId,
        String username,
        String jti,
        int tokenVersion,
        Instant expiration
) {
}
