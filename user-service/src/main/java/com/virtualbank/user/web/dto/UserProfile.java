package com.virtualbank.user.web.dto;

import java.time.Instant;

public record UserProfile(
        String id,
        String username,
        String email,
        String fullName,
        Instant createdAt) {
}
