package com.beetloop.catalog.shared.util;

import java.security.SecureRandom;
import java.time.Instant;

/** Prefixed, roughly time-ordered ids: prd_01HN8ZQ4K7M2X9, var_01HN9C4P, cfg_01HNA5. */
public final class Ids {

    private static final char[] ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private Ids() {
    }

    public static String newId(String prefix) {
        return prefix + "_" + ulid();
    }

    private static String ulid() {
        StringBuilder sb = new StringBuilder(26);
        long time = Instant.now().toEpochMilli();
        char[] timeChars = new char[10];
        for (int i = 9; i >= 0; i--) {
            timeChars[i] = ALPHABET[(int) (time & 31)];
            time >>>= 5;
        }
        sb.append(timeChars);
        for (int i = 0; i < 16; i++) {
            sb.append(ALPHABET[RANDOM.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
