package com.example.telegramtranscription.dto.groq;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response payload from Groq's OpenAI-compatible Chat Completions API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GroqChatCompletionResponse(
        @JsonProperty("id") String id,
        @JsonProperty("choices") List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("index") Integer index,
            @JsonProperty("message") Message message,
            @JsonProperty("finish_reason") String finishReason
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(
            @JsonProperty("role") String role,
            @JsonProperty("content") String content
    ) {
    }

    public String firstContent() {
        if (choices == null || choices.isEmpty() || choices.get(0).message() == null) {
            return null;
        }
        return choices.get(0).message().content();
    }
}
