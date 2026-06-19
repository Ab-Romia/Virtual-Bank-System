package com.virtualbank.account.domain;

/** Lifecycle state of an account, persisted as its name string. Only ACTIVE accounts can move money. */
public enum AccountStatus {
    ACTIVE,
    FROZEN
}
