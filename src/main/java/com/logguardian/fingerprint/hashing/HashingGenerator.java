package com.logguardian.fingerprint.hashing;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashingGenerator {

    private static final String HASHING_ALGORITHM = "SHA-256";

    public String hash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASHING_ALGORITHM);
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not generate fingerprint hash", e);
        }
    }
}
