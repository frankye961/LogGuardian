package com.logguardian.parser.model;

import java.time.Instant;
import java.util.Map;

public record LogEvent(

        String sourceType,
        String sourceId,
        String sourceName,
        Instant ingestTime,
        Instant eventTime,
        LogLevel level,
        String message,
        String fingerprint,
        Map<String, String> attributes

) {}
