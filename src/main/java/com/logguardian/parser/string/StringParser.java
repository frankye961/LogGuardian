package com.logguardian.parser.string;

import com.logguardian.model.LogEntry;
import com.logguardian.parser.BaseParser;
import com.logguardian.parser.model.LogEvent;
import com.logguardian.parser.model.LogLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StringParser implements BaseParser {

    private static final Pattern TIMESTAMP_PATTERN =
            Pattern.compile("\\b(?<ts>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2}(?:[.,]\\d{3})?(?:Z|[+-]\\d{2}:?\\d{2})?)\\b");

    @Override
    public LogEvent parse(LogEntry entry) {
        String raw = entry.message();
        String firstLine = extractFirstLine(raw);

        Instant eventTime = extractTimestamp(firstLine);
        LogLevel level = detectLevel(firstLine);

        return buildEvent(entry, eventTime, level, raw);
    }

    private LogEvent buildEvent(LogEntry entry,
                                Instant eventTime,
                                LogLevel level,
                                String message) {
        return new LogEvent(
                "DOCKER",
                entry.containerId(),
                null,
                entry.seen(),
                eventTime,
                level,
                message,
                null,
                Map.of()
        );
    }

    private String extractFirstLine(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }

        int idx = raw.indexOf('\n');
        if (idx < 0) {
            return raw.trim();
        }

        return raw.substring(0, idx).trim();
    }

    private Instant extractTimestamp(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }

        Matcher matcher = TIMESTAMP_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String ts = matcher.group("ts");

        try {
            ts = ts.replace(",", ".");
            String normalized = ts.replace(" ", "T");

            if (hasExplicitOffset(normalized)) {
                return OffsetDateTime.parse(normalized).toInstant();
            }

            return LocalDateTime.parse(normalized)
                    .atOffset(ZoneOffset.UTC)
                    .toInstant();
        } catch (Exception e) {
            try {
                return OffsetDateTime.parse(ts).toInstant();
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private boolean hasExplicitOffset(String timestamp) {
        if (timestamp.endsWith("Z")) {
            return true;
        }

        int length = timestamp.length();
        if (length < 5) {
            return false;
        }

        for (int index = Math.max(0, length - 6); index < length; index++) {
            char current = timestamp.charAt(index);
            if ((current == '+' || current == '-') && hasOffsetSuffix(timestamp, index + 1)) {
                return true;
            }
        }

        return false;
    }

    private LogLevel detectLevel(String line) {
        if (line == null || line.isBlank()) {
            return LogLevel.UNKNOWN;
        }

        if (containsTokenIgnoreCase(line, "ERROR")) {
            return LogLevel.ERROR;
        }
        if (containsTokenIgnoreCase(line, "WARN") || containsTokenIgnoreCase(line, "WARNING")) {
            return LogLevel.WARN;
        }
        if (containsTokenIgnoreCase(line, "INFO")) {
            return LogLevel.INFO;
        }
        if (containsTokenIgnoreCase(line, "DEBUG")) {
            return LogLevel.DEBUG;
        }
        if (containsTokenIgnoreCase(line, "TRACE")) {
            return LogLevel.TRACE;
        }

        if (containsTokenIgnoreCase(line, "EXCEPTION")) {
            return LogLevel.ERROR;
        }

        return LogLevel.UNKNOWN;
    }

    private boolean hasOffsetSuffix(String timestamp, int startIndex) {
        int remaining = timestamp.length() - startIndex;
        if (remaining == 4) {
            return isDigit(timestamp, startIndex)
                    && isDigit(timestamp, startIndex + 1)
                    && isDigit(timestamp, startIndex + 2)
                    && isDigit(timestamp, startIndex + 3);
        }
        if (remaining == 5) {
            return isDigit(timestamp, startIndex)
                    && isDigit(timestamp, startIndex + 1)
                    && timestamp.charAt(startIndex + 2) == ':'
                    && isDigit(timestamp, startIndex + 3)
                    && isDigit(timestamp, startIndex + 4);
        }
        return false;
    }

    private boolean isDigit(String value, int index) {
        return index < value.length() && Character.isDigit(value.charAt(index));
    }

    private boolean containsTokenIgnoreCase(String line, String token) {
        int maxStart = line.length() - token.length();
        for (int index = 0; index <= maxStart; index++) {
            if (line.regionMatches(true, index, token, 0, token.length())
                    && isTokenBoundary(line, index - 1)
                    && isTokenBoundary(line, index + token.length())) {
                return true;
            }
        }
        return false;
    }

    private boolean isTokenBoundary(String line, int index) {
        if (index < 0 || index >= line.length()) {
            return true;
        }
        return !Character.isLetterOrDigit(line.charAt(index));
    }
}
