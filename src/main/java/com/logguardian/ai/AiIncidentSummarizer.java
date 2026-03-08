package com.logguardian.ai;

import com.logguardian.ai.model.IncidentSeverity;
import com.logguardian.ai.model.IncidentSummary;
import com.logguardian.ai.model.IncidentSummaryRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class AiIncidentSummarizer {

    private final ChatClient chatClient;

    public AiIncidentSummarizer(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public IncidentSummary summarize(IncidentSummaryRequest request) {
        String prompt = buildPrompt(request);
        log.info("Sending AI prompt for fingerprint={} count={}", request.fingerprint(), request.count());

        try {
            String aiResponse = chatClient
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();

            log.info("AI raw response: {}", aiResponse);

            if (aiResponse == null || aiResponse.isBlank()) {
                return new IncidentSummary(
                        "AI Incident Analysis",
                        "No response content returned by model",
                        "Unknown",
                        "Check model configuration and API response",
                        IncidentSeverity.UNKNOWN
                );
            }

            return mapToSummary(aiResponse);
        } catch (Exception e) {
            log.error("AI summarization failed for fingerprint={}", request.fingerprint(), e);
            return new IncidentSummary(
                    "AI Incident Analysis Failed",
                    "The AI model call failed",
                    e.getClass().getSimpleName(),
                    "Check API key, model name, network connectivity, and Spring AI configuration",
                    IncidentSeverity.UNKNOWN
            );
        }
    }

    private String buildPrompt(IncidentSummaryRequest request) {
        return """
                You are analyzing an application incident detected from logs.

                Fingerprint: %s
                Level: %s
                Occurrences in current minute: %d
                Source: %s

                Sample logs:
                %s

                Provide:
                1. A short title
                2. A short summary of what likely happened
                3. The probable root cause
                4. Suggested investigation steps
                5. Severity (LOW, MEDIUM, HIGH)

                Format the response as plain text.
                """.formatted(
                request.fingerprint(),
                request.level(),
                request.count(),
                request.sourceName(),
                formatSamples(request)
        );
    }

    private String formatSamples(IncidentSummaryRequest request) {
        if (request.sampleMessages() == null || request.sampleMessages().isEmpty()) {
            return "No sample logs available";
        }

        StringBuilder builder = new StringBuilder();
        int index = 1;
        for (String msg : request.sampleMessages()) {
            builder.append(index++).append(". ").append(msg).append("\n");
        }
        return builder.toString();
    }

    private IncidentSummary mapToSummary(String response) {
        return new IncidentSummary(
                "AI Incident Analysis",
                response,
                "See AI analysis",
                "See AI analysis",
                IncidentSeverity.UNKNOWN
        );
    }
}