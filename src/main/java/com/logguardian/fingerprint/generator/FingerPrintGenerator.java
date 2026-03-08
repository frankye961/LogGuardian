package com.logguardian.fingerprint.generator;

import com.logguardian.fingerprint.hashing.HashingGenerator;
import com.logguardian.fingerprint.normalize.NormalizationGenerator;
import com.logguardian.parser.model.LogEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FingerPrintGenerator {

    private final HashingGenerator hashingGenerator;
    private final NormalizationGenerator normalizationGenerator;

    public LogEvent generateFingerprint(LogEvent event){
        String raw = event.message();
        String normalized = normalizationGenerator.normalize(raw);
        String hashedFingerprint = hashingGenerator.hash(normalized);


        return new LogEvent(
                event.sourceType(),
                event.sourceId(),
                event.sourceName(),
                event.ingestTime(),
                event.eventTime(),
                event.level(),
                event.message(),
                hashedFingerprint,
                event.attributes()
        );
    }
}
