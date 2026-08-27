package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.GroqClient;
import com.example.telegramtranscription.config.GroqTranscriptionProperties;
import com.example.telegramtranscription.config.TranscriptionMode;
import com.example.telegramtranscription.dto.groq.GroqTranscriptionResponse;
import com.example.telegramtranscription.exception.GroqApiException;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Business logic layer around Groq speech-to-text transcription.
 * Supports configurable transcription strategies:
 * 1. Forced language mode (default: Telugu "te")
 * 2. Multi-language detection & filtering mode (e.g. Telugu + English only, dropping other languages)
 */
@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    private final GroqClient groqClient;
    private final GroqTranscriptionProperties properties;

    public TranscriptionService(GroqClient groqClient, GroqTranscriptionProperties properties) {
        this.groqClient = groqClient;
        this.properties = properties;
    }

    /**
     * Primary entry point. Transcribes audio based on the configured mode in GroqTranscriptionProperties.
     * Returns an empty Mono if the language is filtered out.
     */
    public Mono<String> transcribe(AudioFile audioFile) {
        if (audioFile == null || audioFile.bytes() == null || audioFile.bytes().length == 0) {
            return Mono.error(new GroqApiException("Cannot transcribe empty audio file"));
        }

        if (properties.mode() == TranscriptionMode.FILTERED_LANGUAGES) {
            return transcribeWithLanguageFilter(audioFile, properties.allowedLanguages());
        }

        return transcribeForcedLanguage(audioFile, properties.language());
    }

    /**
     * Forcibly transcribes audio into the specified language (e.g. "te" for Telugu).
     */
    public Mono<String> transcribeForcedLanguage(AudioFile audioFile, String language) {
        if (audioFile == null || audioFile.bytes() == null || audioFile.bytes().length == 0) {
            return Mono.error(new GroqApiException("Cannot transcribe empty audio file"));
        }

        return groqClient.transcribeForcedLanguage(audioFile, language)
                .map(this::extractText)
                .doOnNext(text -> log.debug("Forced language ({}) transcription result length={}", language, text.length()));
    }

    /**
     * Transcribes audio using Whisper auto-detection and only accepts the result if the detected language
     * is in the allowed languages set (e.g., Telugu and English). Discards other languages by returning Mono.empty().
     */
    public Mono<String> transcribeWithLanguageFilter(AudioFile audioFile, Collection<String> allowedLanguages) {
        if (audioFile == null || audioFile.bytes() == null || audioFile.bytes().length == 0) {
            return Mono.error(new GroqApiException("Cannot transcribe empty audio file"));
        }

        Set<String> normalizedAllowed = allowedLanguages == null ? Set.of() :
                allowedLanguages.stream()
                        .map(String::trim)
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());

        return groqClient.transcribeVerbose(audioFile)
                .flatMap(response -> {
                    String detectedLang = response.language();
                    String normalizedDetected = detectedLang != null ? detectedLang.trim().toLowerCase(Locale.ROOT) : "";

                    if (!normalizedAllowed.contains(normalizedDetected)) {
                        log.info("Audio ignored: detected language '{}' is not in allowed list {}",
                                detectedLang, normalizedAllowed);
                        return Mono.empty();
                    }

                    String text = extractText(response);
                    log.debug("Language '{}' accepted, transcription result length={}", detectedLang, text.length());
                    return Mono.just(text);
                });
    }

    private String extractText(GroqTranscriptionResponse response) {
        String text = response != null ? response.text() : null;
        return (text == null || text.isBlank()) ? "(no speech detected)" : text.trim();
    }
}

