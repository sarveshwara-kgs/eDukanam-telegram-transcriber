package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Telegram "Voice" object (voice note, always OGG/Opus).
 * https://core.telegram.org/bots/api#voice
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramVoice(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_size") Long fileSize
) {
}
