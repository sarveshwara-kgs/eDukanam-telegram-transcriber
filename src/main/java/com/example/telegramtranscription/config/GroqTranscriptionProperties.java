package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for Groq transcription behavior.
 *
 * @param mode             The transcription mode (FORCED_LANGUAGE or FILTERED_LANGUAGES).
 * @param language         The default language code to force (e.g. "te" for Telugu).
 * @param allowedLanguages Comma-separated or list of allowed language identifiers (e.g. ["te", "telugu", "en", "english"]).
 */
@ConfigurationProperties(prefix = "groq.transcription")
public record GroqTranscriptionProperties(
        TranscriptionMode mode,
        String language,
        List<String> allowedLanguages
) {
    public GroqTranscriptionProperties {
        if (mode == null) {
            mode = TranscriptionMode.FORCED_LANGUAGE;
        }
        if (language == null || language.isBlank()) {
            language = "te";
        }
        if (allowedLanguages == null || allowedLanguages.isEmpty()) {
            allowedLanguages = List.of("te", "telugu", "en", "english");
        }
    }
}
