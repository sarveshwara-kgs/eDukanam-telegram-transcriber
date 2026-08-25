package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds "groq.api.*" properties from application.yml.
 */
@ConfigurationProperties(prefix = "groq.api")
public record GroqProperties(
        String key,
        String baseUrl,
        String model
) {
}
