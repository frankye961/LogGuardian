package com.logguardian.ai.model;


import com.logguardian.parser.model.LogLevel;

import java.time.Instant;
import java.util.List;

public record IncidentSummaryRequest(
        String fingerprint,
        LogLevel level,
        int count,
        String sourceId,
        String sourceName,
        List<String> sampleMessages
) {

    public IncidentSummaryRequest {
        sampleMessages = sampleMessages == null ? List.of() : List.copyOf(sampleMessages);
    }
}
