package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds "telegram.bot.*" properties from application.yml.
 */
@ConfigurationProperties(prefix = "telegram.bot")
public record TelegramProperties(
        String token,
        String apiBaseUrl,
        String webhookSecret
) {
}
