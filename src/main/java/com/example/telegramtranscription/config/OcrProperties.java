package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the OCR Vision provider (e.g. OpenRouter).
 */
@ConfigurationProperties(prefix = "ocr.api")
public record OcrProperties(
        String key,
        String baseUrl,
        String model
) {
    public OcrProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "https://openrouter.ai/api/v1";
        }
        if (model == null || model.isBlank()) {
            model = "qwen/qwen-2.5-vl-72b-instruct";
        }
    }
}
