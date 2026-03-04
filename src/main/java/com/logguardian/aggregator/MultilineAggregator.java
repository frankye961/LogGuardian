package com.logguardian.aggregator;

import com.logguardian.model.LogEntry;
import com.logguardian.model.LogLine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MultilineAggregator {

    private static final int MAX_LINES = 500;
    private static final long FLUSH_TIME = 10000;

    public Flux<LogEntry> transform(Flux<LogLine> lineFlux) {
        return lineFlux.
                windowUntil(logLine -> isNewEntryStart(logLine.line()))
                .flatMap(window -> window
                        .windowTimeout(MAX_LINES, Duration.ofMillis(FLUSH_TIME))
                        .concatMap(chunk ->
                                chunk.collectList()
                                        .filter(list -> !list.isEmpty())
                                        .map(list -> {
                                            LogLine first = list.getFirst();
                                            String joined = list.stream()
                                                    .map(LogLine::line)
                                                    .collect(Collectors.joining("\n"));

                                            return new LogEntry(
                                                    first.containerId(),
                                                    first.receivedAt(),
                                                    joined
                                            );
                                        })
                        )
                );
    }

    private boolean isNewEntryStart(String msg) {
        if (msg == null || msg.isBlank()) return true;
        if (msg.startsWith(" ") || msg.startsWith("\t")) return false;
        if (msg.startsWith("at ")) return false;
        if (msg.startsWith("Caused by:")) return false;
        if (msg.startsWith("...")) return false;
        return true;
    }

}
