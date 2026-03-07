package com.logguardian.parser.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.logguardian.model.LogEntry;
import com.logguardian.parser.BaseParser;
import com.logguardian.parser.model.LogEvent;

import com.logguardian.parser.model.LogLevel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Component
public class JsonParser implements BaseParser {

    private final ObjectMapper mapper;

    @Autowired
    public JsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LogEvent parse(LogEntry entry) {
        try {
            Map<String, Object> json = mapper.readValue(
                    entry.message(),
                    new TypeReference<Map<String, Object>>() {}
            );

            Instant eventTime = extractTimestamp(json);

            return new LogEvent(
                    "DOCKER",
                    entry.containerId(),
                    null,
                    entry.seen(),
                    eventTime,
                    LogLevel.UNKNOWN,
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
