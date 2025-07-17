package com.virtualbank.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private UUID userId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private OffsetDateTime createdAt;
    private Boolean isActive;
}