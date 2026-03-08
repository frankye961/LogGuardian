package com.logguardian.fingerprint.anomaly;

import com.logguardian.parser.model.LogLevel;

import java.time.Instant;

public record AnomalyEvent(
        String fingerprint,
        LogLevel level,
        String sourceId,
        String sourceName,
        Instant detectedAt,
        int count,
        String sampleMessage
) {}
