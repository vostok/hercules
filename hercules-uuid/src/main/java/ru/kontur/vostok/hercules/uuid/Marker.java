package ru.kontur.vostok.hercules.uuid;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 12 bit marker is constructed from key string as follows:                                                         <br>
 * 1. Get UTF-8 bytes from key                                                                                      <br>
 * 2. Compute SHA-512 hash                                                                                          <br>
 * 3. Get 12 most significant bits as the marker                                                                    <br>
 * 4. Otherwise, zero will be used                                                                                  <br>
 * @author Gregory Koshelev
 */
public class Marker {
    public static final Marker EMPTY = new Marker(0L);

    private final long marker;

    private Marker(long marker) {
        this.marker = marker;
    }

    public long get() {
        return marker;
    }

    public static Marker forKey(String key) {
        if (key == null) {
            return new Marker(0x000000000000FFFL);
        }

        return new Marker(trim(hash(key.getBytes(StandardCharsets.UTF_8))));
    }

    private static byte[] hash(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(bytes);
            return md.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private static long trim(byte[] bytes) {
        if (bytes.length >= 2) {
            return (bytes[0] << 4) | (bytes[1] & 0x0F);
        }
        if (bytes.length == 1) {
            return bytes[0] << 4;
        }
        return 0L;
    }
}
