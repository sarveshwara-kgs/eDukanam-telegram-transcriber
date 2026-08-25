package com.example.telegramtranscription.dto.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Groq's OpenAI-compatible
 * POST /openai/v1/audio/transcriptions endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroqTranscriptionResponse(
        @JsonProperty("text") String text
) {
}
