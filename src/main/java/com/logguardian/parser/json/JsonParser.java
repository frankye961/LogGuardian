package com.logguardian.parser.json;

import com.logguardian.model.LogEntry;
import com.logguardian.parser.BaseParser;
import com.logguardian.parser.model.LogEvent;
import com.logguardian.parser.model.LogLevel;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Component
public class JsonParser implements BaseParser {

    private static final Pattern ERROR_PATTERN = Pattern.compile("\\bERROR\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern WARN_PATTERN = Pattern.compile("\\bWARN(?:ING)?\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern INFO_PATTERN = Pattern.compile("\\bINFO\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DEBUG_PATTERN = Pattern.compile("\\bDEBUG\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRACE_PATTERN = Pattern.compile("\\bTRACE\\b", Pattern.CASE_INSENSITIVE);

    private final ObjectMapper mapper;

    public JsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LogEvent parse(LogEntry entry) {
        try {
            Map<String, Object> json = mapper.readValue(entry.message(), Map.class);

            Instant eventTime = extractTimestamp(json);
            LogLevel level = extractLevel(json, entry.message());

            return new LogEvent(
                    "DOCKER",
                    entry.containerId(),
                    null,
                    entry.seen(),
                    eventTime,
                    level,
                    entry.message(),
                    null,
                    Map.of()
            );

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON log", e);
        }
    }

    private Instant extractTimestamp(Map<String, Object> json) {
        String rawTs = firstNonNull(json, "@timestamp", "timestamp", "ts", "time");

        if (rawTs == null || rawTs.isBlank()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(rawTs).toInstant();
        } catch (Exception e) {
            try {
                return Instant.parse(rawTs);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private LogLevel extractLevel(Map<String, Object> json, String rawMessage) {
        // 1. Try common keys first
        String rawLevel = firstNonNull(json, "level", "severity", "level", "lvl");
        LogLevel keyLevel = toLevel(rawLevel);
        if (keyLevel != LogLevel.UNKNOWN) {
            return keyLevel;
        }

        // 2. Scan all JSON values
        for (Object value : json.values()) {
            if (value == null) {
                continue;
            }
            LogLevel detected = detectLevelFromText(String.valueOf(value));
            if (detected != LogLevel.UNKNOWN) {
                return detected;
            }
        }

        // 3. Scan the full raw log string
        return detectLevelFromText(rawMessage);
    }

    private LogLevel detectLevelFromText(String text) {
        if (text == null || text.isBlank()) {
            return LogLevel.UNKNOWN;
        }

        if (ERROR_PATTERN.matcher(text).find()) {
            return LogLevel.ERROR;
        }
        if (WARN_PATTERN.matcher(text).find()) {
            return LogLevel.WARN;
        }
        if (INFO_PATTERN.matcher(text).find()) {
            return LogLevel.INFO;
        }
        if (DEBUG_PATTERN.matcher(text).find()) {
            return LogLevel.DEBUG;
        }
        if (TRACE_PATTERN.matcher(text).find()) {
            return LogLevel.TRACE;
        }

        return LogLevel.UNKNOWN;
    }

    private LogLevel toLevel(String rawLevel) {
        if (rawLevel == null || rawLevel.isBlank()) {
            return LogLevel.UNKNOWN;
        }

        String normalized = rawLevel.trim().toUpperCase(Locale.ROOT);

        return switch (normalized) {
            case "ERROR" -> LogLevel.ERROR;
            case "WARN", "WARNING" -> LogLevel.WARN;
            case "INFO" -> LogLevel.INFO;
            case "DEBUG" -> LogLevel.DEBUG;
            case "TRACE" -> LogLevel.TRACE;
            default -> LogLevel.UNKNOWN;
        };
    }

    private String firstNonNull(Map<String, Object> json, String... keys) {
        for (String key : keys) {
            Object value = json.get(key);
            if (value != null) {
                return String.valueOf(value);
            }
        }
        return null;
    }
}