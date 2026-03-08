package com.logguardian.mapper;

import com.logguardian.ai.model.IncidentSummaryRequest;
import com.logguardian.fingerprint.anomaly.AnomalyEvent;

import java.util.List;

public class AiSummerizerMapper {

    public static IncidentSummaryRequest toIncidentSummaryRequest(AnomalyEvent anomaly) {
        return new IncidentSummaryRequest(
                anomaly.fingerprint(),
                anomaly.level(),
                anomaly.count(),
                anomaly.sourceId(),
                anomaly.sourceName(),
                List.of(anomaly.sampleMessage())
        );
    }
}
