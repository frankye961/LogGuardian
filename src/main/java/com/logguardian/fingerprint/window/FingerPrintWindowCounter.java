package com.logguardian.fingerprint.window;

import com.logguardian.parser.model.LogEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FingerPrintWindowCounter {

    private static final int RETAINED_WINDOWS_PER_FINGERPRINT = 2;

    private final Map<String, Map<Long, Integer>> eventsInWindowTime = new ConcurrentHashMap<>();
    private final long normalizedWindowSeconds;

    public FingerPrintWindowCounter(@Value("${logguardian.detection.window-seconds:60}") int windowSeconds) {
        this.normalizedWindowSeconds = Math.max(1, windowSeconds);
    }

    public int countFingerprint(LogEvent event) {

        Instant timestamp = event.eventTime() != null
                ? event.eventTime()
                : event.ingestTime();

        long windowStart = calculateWindowStart(timestamp);

        Map<Long, Integer> windowMap =
                eventsInWindowTime.computeIfAbsent(event.fingerprint(), k -> new ConcurrentHashMap<>());

        int count = windowMap.merge(windowStart, 1, Integer::sum);
        if (windowMap.size() > RETAINED_WINDOWS_PER_FINGERPRINT + 1) {
            evictExpiredWindows(windowMap, windowStart);
        }
        return count;
    }

    private long calculateWindowStart(Instant timestamp) {
        long epochSeconds = timestamp.getEpochSecond();
        return epochSeconds - Math.floorMod(epochSeconds, normalizedWindowSeconds);
    }

    private void evictExpiredWindows(Map<Long, Integer> windowMap, long currentWindowStart) {
        long oldestRetainedWindow = currentWindowStart - (normalizedWindowSeconds * RETAINED_WINDOWS_PER_FINGERPRINT);
        Iterator<Long> iterator = windowMap.keySet().iterator();
        while (iterator.hasNext()) {
            Long windowStart = iterator.next();
            if (windowStart < oldestRetainedWindow) {
                iterator.remove();
            }
        }
    }
}
