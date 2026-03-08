package com.logguardian.fingerprint.normalize;

import java.util.regex.Pattern;

public final class NormalizationRules {

    public static final Pattern UUID_PATTERN =
            Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");

    public static final Pattern IP_PATTERN =
            Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");

    public static final Pattern HEX_PATTERN =
            Pattern.compile("\\b[a-fA-F0-9]{16,}\\b");

    public static final Pattern NUMBER_PATTERN =
            Pattern.compile("\\b\\d+\\b");
}
