package com.virtualbank.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;

/**
 * RestClients the tools use to read the caller's banking data from the other
 * services. Each outbound call carries the caller's own bearer token, so the
 * downstream services apply their normal ownership checks and the assistant can
 * never see anyone else's data. The token is read from the validated JWT in the
 * security context at call time rather than from the request, which keeps the
 * caller's identity authoritative end to end.
 */
@Configuration
public class BankingClientConfig {

    /** Base URL for account-service. Defaults to the per-service local port. */
    private final String accountBaseUrl;

    /** Base URL for transaction-service. Defaults to the per-service local port. */
    private final String transactionBaseUrl;

    public BankingClientConfig(
            @Value("${assistant.account-service-uri:${ACCOUNT_SERVICE_URI:http://localhost:8082}}") String accountBaseUrl,
            @Value("${assistant.transaction-service-uri:${TRANSACTION_SERVICE_URI:http://localhost:8083}}") String transactionBaseUrl) {
        this.accountBaseUrl = accountBaseUrl;
        this.transactionBaseUrl = transactionBaseUrl;
    }

    @Bean
    public RestClient accountServiceClient(RestClient.Builder builder) {
        return bearerForwardingClient(builder, accountBaseUrl);
    }

    @Bean
    public RestClient transactionServiceClient(RestClient.Builder builder) {
        return bearerForwardingClient(builder, transactionBaseUrl);
    }

    private RestClient bearerForwardingClient(RestClient.Builder builder, String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .requestInterceptor((request, body, execution) -> {
                    String token = currentToken();
                    if (token != null) {
                        request.getHeaders().set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
                    }
                    return execution.execute(request, body);
                })
                .build();
    }

    private static String currentToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getTokenValue();
        }
        return null;
    }
}
