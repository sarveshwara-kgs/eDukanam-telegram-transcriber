package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.GroqClient;
import com.example.telegramtranscription.exception.GroqApiException;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Business logic layer around Groq speech-to-text transcription.
 * Isolated from the Telegram-specific code so it could, in principle,
 * be reused for other audio sources later.
 */
@Service
public class TranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(TranscriptionService.class);

    private final GroqClient groqClient;

    public TranscriptionService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    /**
     * Transcribes the given audio file using Groq and returns the plain text.
     */
    public Mono<String> transcribe(AudioFile audioFile) {
        if (audioFile == null || audioFile.bytes() == null || audioFile.bytes().length == 0) {
            return Mono.error(new GroqApiException("Cannot transcribe empty audio file"));
        }

        return groqClient.transcribe(audioFile)
                .map(response -> {
                    String text = response.text();
                    return (text == null || text.isBlank()) ? "(no speech detected)" : text.trim();
                })
                .doOnNext(text -> log.debug("Transcription result length={}", text.length()));
    }
}
