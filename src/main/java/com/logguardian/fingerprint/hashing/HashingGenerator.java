package com.logguardian.fingerprint.hashing;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class HashingGenerator {

    private static final String HASHING_ALGORITHM = "SHA-256";
    private static final HexFormat HEX = HexFormat.of();
    private static final ThreadLocal<MessageDigest> DIGEST =
            ThreadLocal.withInitial(HashingGenerator::createDigest);

    public String hash(String input) {
        MessageDigest digest = DIGEST.get();
        digest.reset();
        byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        return HEX.formatHex(hashBytes);
    }

    private static MessageDigest createDigest() {
        try {
            return MessageDigest.getInstance(HASHING_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Could not generate fingerprint hash", e);
        }
    }
}
