package com.twitter.ums.contracts;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record LoginResponse(
        long userId,
        String login,
        String email,
        String name,
        LocalDate birthDate,
        List<RoleResponse> roles,
        long sessionId,
        Instant expiresAt
) {
}
