package com.tuhoang.pocketmind.utils;

import java.security.SecureRandom;

public class PaymentUtils {
    
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom random = new SecureRandom();

    /**
     * Generates a secure, readable payment code (e.g., PM-A9B4X2)
     */
    public static String generatePaymentCode() {
        StringBuilder sb = new StringBuilder("PM-");
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}
