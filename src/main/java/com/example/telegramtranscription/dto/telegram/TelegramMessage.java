package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Telegram "Message" object.
 * https://core.telegram.org/bots/api#message
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramMessage(
        @JsonProperty("message_id") Long messageId,
        @JsonProperty("chat") TelegramChat chat,
        @JsonProperty("voice") TelegramVoice voice,
        @JsonProperty("audio") TelegramAudio audio,
        @JsonProperty("text") String text
) {
}
