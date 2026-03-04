package com.logguardian.parser.string;

import com.logguardian.model.LogEntry;
import com.logguardian.parser.BaseParser;
import com.logguardian.parser.model.LogEvent;
import com.logguardian.parser.model.LogLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class StringParser implements BaseParser {

    private static final Pattern TIMESTAMP_LEVEL_PATTERN =
            Pattern.compile("^(?<ts>\\d{4}-\\d{2}-\\d{2}[ T]\\d{2}:\\d{2}:\\d{2})\\s+(?<level>ERROR|WARN|INFO|DEBUG|TRACE)\\s+(?<msg>.*)$");

    private static final Pattern BRACKET_LEVEL_PATTERN =
            Pattern.compile("^\\[(?<level>ERROR|WARN|INFO|DEBUG|TRACE)]\\s+(?<msg>.*)$");

    private static final Pattern LEVEL_ONLY_PATTERN =
            Pattern.compile("^(?<level>ERROR|WARN|INFO|DEBUG|TRACE)\\s+(?<msg>.*)$");

    @Override
    public LogEvent parse(LogEntry entry) {

        String raw = entry.message();

        Matcher m = TIMESTAMP_LEVEL_PATTERN.matcher(raw);
        if (m.find()) {
            return buildEvent(entry,
                    parseInstant(m.group("ts")),
                    toLevel(m.group("level")),
                    m.group("msg"));
        }

        m = BRACKET_LEVEL_PATTERN.matcher(raw);
        if (m.find()) {
            return buildEvent(entry,
                    null,
                    toLevel(m.group("level")),
                    m.group("msg"));
        }

        m = LEVEL_ONLY_PATTERN.matcher(raw);
        if (m.find()) {
            return buildEvent(entry,
                    null,
                    toLevel(m.group("level")),
                    m.group("msg"));
        }

        return buildEvent(entry,
                null,
                LogLevel.UNKNOWN,
                raw);
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

    private Instant parseInstant(String ts) {
        return LocalDateTime.parse(ts.replace(" ", "T"))
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    private LogLevel toLevel(String level) {
        return switch (level) {
            case "ERROR" -> LogLevel.ERROR;
            case "WARN" -> LogLevel.WARN;
            case "INFO" -> LogLevel.INFO;
            case "DEBUG" -> LogLevel.DEBUG;
            case "TRACE" -> LogLevel.TRACE;
            default -> LogLevel.UNKNOWN;
        };
    }
}
