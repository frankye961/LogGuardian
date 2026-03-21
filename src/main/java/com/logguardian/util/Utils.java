package com.logguardian.util;

public final class Utils {

    private Utils() {
    }

    public static boolean checkIfJson(String message) {
        if (message == null) {
            return false;
        }

        String trimmed = message.trim();
        return trimmed.startsWith("{") && trimmed.endsWith("}");
    }
}
