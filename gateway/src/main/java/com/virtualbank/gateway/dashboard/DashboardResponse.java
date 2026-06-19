package com.virtualbank.gateway.dashboard;

import java.util.List;

/**
 * Aggregated dashboard payload for the authenticated caller. The profile is fetched
 * from user-service. Accounts and recent transactions are placeholders today and will
 * be populated once account-service and transaction-service are available; modelling
 * them now keeps the response shape stable for the frontend.
 *
 * @param profile      the caller's user profile, or {@code null} if user-service is unreachable
 * @param accounts     the caller's accounts (empty until account-service exists)
 * @param transactions recent transactions (empty until transaction-service exists)
 */
public record DashboardResponse(
        Object profile,
        List<Object> accounts,
        List<Object> transactions) {

    /** Builds a dashboard with the given profile and empty downstream collections. */
    public static DashboardResponse of(Object profile) {
        return new DashboardResponse(profile, List.of(), List.of());
    }

}
