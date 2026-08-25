package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Telegram "Audio" object (music/audio files sent as documents,
 * as opposed to recorded voice notes). Supported for convenience so users
 * can also send audio files, not just voice notes.
 * https://core.telegram.org/bots/api#audio
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramAudio(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        @JsonProperty("duration") Integer duration,
        @JsonProperty("mime_type") String mimeType,
        @JsonProperty("file_name") String fileName,
        @JsonProperty("file_size") Long fileSize
) {
}
