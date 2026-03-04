package com.logguardian.parser;

import com.logguardian.model.LogEntry;
import com.logguardian.parser.model.LogEvent;

public interface BaseParser {
    LogEvent parse(LogEntry entry);
}
