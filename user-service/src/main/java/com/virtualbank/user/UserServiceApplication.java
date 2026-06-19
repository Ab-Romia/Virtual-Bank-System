package com.virtualbank.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Identity service: it issues the RS256 tokens the rest of the platform trusts and
 * publishes the JWKS those services validate against. Shared concerns (exception
 * handling, security defaults, audit publishing) arrive through vbank-common's
 * auto-configuration rather than component scanning, so this service only owns
 * its own package.
 */
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
