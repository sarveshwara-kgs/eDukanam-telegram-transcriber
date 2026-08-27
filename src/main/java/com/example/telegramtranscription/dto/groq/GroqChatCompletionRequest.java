package com.example.telegramtranscription.dto.groq;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Request payload for Groq's OpenAI-compatible Chat Completions API with Vision.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GroqChatCompletionRequest(
        @JsonProperty("model") String model,
        @JsonProperty("messages") List<ChatMessage> messages,
        @JsonProperty("max_tokens") Integer maxTokens,
        @JsonProperty("temperature") Double temperature
) {
    public record ChatMessage(
            @JsonProperty("role") String role,
            @JsonProperty("content") List<MessageContent> content
    ) {
    }

    public record MessageContent(
            @JsonProperty("type") String type,
            @JsonProperty("text") String text,
            @JsonProperty("image_url") ImageUrl imageUrl
    ) {
        public static MessageContent textContent(String text) {
            return new MessageContent("text", text, null);
        }

        public static MessageContent imageUrlContent(String url) {
            return new MessageContent("image_url", null, new ImageUrl(url));
        }
    }

    public record ImageUrl(
            @JsonProperty("url") String url
    ) {
    }
}
