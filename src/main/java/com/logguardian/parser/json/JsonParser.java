package com.logguardian.parser.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.logguardian.model.LogEntry;
import com.logguardian.parser.BaseParser;
import com.logguardian.parser.model.LogEvent;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
public class JsonParser implements BaseParser {

    private final ObjectMapper mapper;

    @Autowired
    public JsonParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LogEvent parse(LogEntry entry) {
        return Optional.ofNullable(mapper.readValue(entry.message(), LogEvent.class)).get();
    }
}
