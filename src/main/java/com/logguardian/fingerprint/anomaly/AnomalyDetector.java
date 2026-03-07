package com.logguardian.fingerprint.anomaly;

import com.logguardian.fingerprint.window.CountedLogEvent;
import com.logguardian.parser.model.LogEvent;
import com.logguardian.parser.model.LogLevel;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component

public class AnomalyDetector {

    private static final int ERROR_THRESHOLD = 20;

    public Optional<AnomalyEvent> detectAnomaly(CountedLogEvent countedEvent){
        LogEvent logEvent = countedEvent.event();

        if(logEvent.level() != LogLevel.ERROR){
            return Optional.empty();
        }

        if (countedEvent.count() <= ERROR_THRESHOLD) {
            return Optional.empty();
        }

        return Optional.of(new AnomalyEvent(
                logEvent.fingerprint(),
                logEvent.level(),
                logEvent.sourceId(),
                logEvent.sourceName(),
                Instant.now(),
                countedEvent.count(),
                logEvent.message()
        ));
    }
}
