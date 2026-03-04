package com.logguardian.model;

import java.time.Instant;

public record LogEntry(String containerId,
                       Instant seen,
                       String message) {}
