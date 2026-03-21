package com.logguardian.fingerprint.window;

import com.logguardian.parser.model.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FingerPrintWindowCounter {

    private final Map<String, Map<Instant, Integer>> eventsInWindowTime = new ConcurrentHashMap<>();
    private final int windowSeconds;

    public FingerPrintWindowCounter(@Value("${logguardian.detection.window-seconds:60}") int windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int countFingerprint(LogEvent event) {

        Instant timestamp = event.eventTime() != null
                ? event.eventTime()
                : event.ingestTime();

        Instant windowStart = calculateWindowStart(timestamp);

        Map<Instant, Integer> windowMap =
                eventsInWindowTime.computeIfAbsent(event.fingerprint(), k -> new ConcurrentHashMap<>());

        return windowMap.merge(windowStart, 1, Integer::sum);
    }

    private Instant calculateWindowStart(Instant timestamp) {
        long normalizedWindowSeconds = Math.max(1, windowSeconds);
        long epochSeconds = timestamp.getEpochSecond();
        return Instant.ofEpochSecond(epochSeconds - Math.floorMod(epochSeconds, normalizedWindowSeconds));
    }
}
