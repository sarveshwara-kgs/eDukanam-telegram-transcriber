package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents one size of a photo or a file / thumbnail.
 * https://core.telegram.org/bots/api#photosize
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramPhotoSize(
        @JsonProperty("file_id") String fileId,
        @JsonProperty("file_unique_id") String fileUniqueId,
        @JsonProperty("width") Integer width,
        @JsonProperty("height") Integer height,
        @JsonProperty("file_size") Long fileSize
) {
}
