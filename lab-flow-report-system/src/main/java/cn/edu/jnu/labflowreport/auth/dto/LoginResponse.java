package cn.edu.jnu.labflowreport.auth.dto;

import java.time.Instant;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        UserProfile user
) {
}

