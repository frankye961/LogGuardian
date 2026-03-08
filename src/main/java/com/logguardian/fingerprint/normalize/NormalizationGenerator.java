package com.logguardian.fingerprint.normalize;

import org.springframework.stereotype.Component;

@Component
public class NormalizationGenerator {

    private final static String UUID_MATCHER = "<uuid>";
    private final static String IP_MATCHER = "<ip>";
    private final static String ID_MATCHER = "<id>";
    private final static String PLACE_HOLDER = "?";

    public String normalize(String message) {
        if (message == null || message.isBlank()) {
            return "";
        }

        String normalized = message;

        normalized = NormalizationRules.UUID_PATTERN.matcher(normalized).replaceAll(UUID_MATCHER);
        normalized = NormalizationRules.IP_PATTERN.matcher(normalized).replaceAll(IP_MATCHER);
        normalized = NormalizationRules.HEX_PATTERN.matcher(normalized).replaceAll(ID_MATCHER);
        normalized = NormalizationRules.NUMBER_PATTERN.matcher(normalized).replaceAll(PLACE_HOLDER);
        normalized = normalized.replaceAll("\\s+", " ").trim();

        return normalized;
    }
}
