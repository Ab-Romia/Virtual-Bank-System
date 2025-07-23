package com.virtualbank.account.service;

import java.util.Random;

public class AccountNumberGenerator {

    private static final int LENGTH = 10;

    public static String generateUniqueAccountNumber() {
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < LENGTH; i++) {
            sb.append(random.nextInt(10)); // digit from 0 to 9
        }
        return sb.toString();
    }
}
