package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response wrapper for the Telegram "getFile" API call.
 * https://core.telegram.org/bots/api#getfile
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramFileResponse(
        @JsonProperty("ok") boolean ok,
        @JsonProperty("result") TelegramFileResult result
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TelegramFileResult(
            @JsonProperty("file_id") String fileId,
            @JsonProperty("file_unique_id") String fileUniqueId,
            @JsonProperty("file_size") Long fileSize,
            @JsonProperty("file_path") String filePath
    ) {
    }
}
