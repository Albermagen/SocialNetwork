package com.socialnetwork.shared.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Generación de UUID v7 (RFC 9562): ordenados temporalmente, mejor localidad en índices B-tree que
 * los v4 aleatorios. Convención del proyecto para todas las PKs (ver docs/DATABASE.md).
 */
public final class Uuids {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Uuids() {}

    public static UUID v7() {
        byte[] value = new byte[16];
        RANDOM.nextBytes(value);

        long timestamp = System.currentTimeMillis();
        value[0] = (byte) (timestamp >>> 40);
        value[1] = (byte) (timestamp >>> 32);
        value[2] = (byte) (timestamp >>> 24);
        value[3] = (byte) (timestamp >>> 16);
        value[4] = (byte) (timestamp >>> 8);
        value[5] = (byte) timestamp;

        value[6] = (byte) ((value[6] & 0x0F) | 0x70); // versión 7
        value[8] = (byte) ((value[8] & 0x3F) | 0x80); // variante RFC

        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (value[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (value[i] & 0xFF);
        }
        return new UUID(msb, lsb);
    }
}
