package com.example.telegramtranscription.dto.telegram;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a Telegram "Chat" object.
 * https://core.telegram.org/bots/api#chat
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TelegramChat(
        @JsonProperty("id") Long id,
        @JsonProperty("type") String type
) {
}
