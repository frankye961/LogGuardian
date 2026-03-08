package com.logguardian.fingerprint.window;

import com.logguardian.parser.model.LogEvent;

import java.time.Instant;

public record CountedLogEvent(
        LogEvent event,
        int count
) {}