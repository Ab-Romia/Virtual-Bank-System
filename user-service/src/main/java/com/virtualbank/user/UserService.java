package com.virtualbank.user;

import com.virtualbank.common.web.ApiException;
import com.virtualbank.user.auth.TokenService;
import com.virtualbank.user.web.dto.LoginRequest;
import com.virtualbank.user.web.dto.LoginResponse;
import com.virtualbank.user.web.dto.RegisterRequest;
import com.virtualbank.user.web.dto.RegisterResponse;
import com.virtualbank.user.web.dto.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/** Registration, credential checks, and profile lookups for users. */
@Service
public class UserService {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public UserService(AppUserRepository users, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw ApiException.conflict("Username already taken");
        }
        AppUser user = new AppUser(
                UUID.randomUUID().toString(),
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName(),
                Instant.now());
        users.save(user);
        return new RegisterResponse(user.getId(), user.getUsername());
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        AppUser user = users.findByUsername(request.username())
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid username or password"));
        TokenService.IssuedToken token = tokenService.issue(user);
        return new LoginResponse(user.getId(), user.getUsername(), token.value(), "Bearer",
                token.expiresInSeconds());
    }

    @Transactional(readOnly = true)
    public UserProfile profile(String userId) {
        AppUser user = users.findById(userId)
                .orElseThrow(() -> ApiException.notFound("User not found"));
        return new UserProfile(user.getId(), user.getUsername(), user.getEmail(),
                user.getFullName(), user.getCreatedAt());
    }
}
