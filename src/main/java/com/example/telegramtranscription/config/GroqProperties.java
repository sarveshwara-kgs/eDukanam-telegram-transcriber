package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds "groq.api.*" properties from application.yml.
 */
@ConfigurationProperties(prefix = "groq.api")
public record GroqProperties(
        String key,
        String baseUrl,
        String model,
        String visionModel
) {
    public GroqProperties {
        if (visionModel == null || visionModel.isBlank()) {
            visionModel = "qwen/qwen3.6-27b";
        }
    }
}
