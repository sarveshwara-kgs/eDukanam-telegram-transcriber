package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.GroqClient;
import com.example.telegramtranscription.config.GroqTranscriptionProperties;
import com.example.telegramtranscription.config.TranscriptionMode;
import com.example.telegramtranscription.dto.groq.GroqTranscriptionResponse;
import com.example.telegramtranscription.model.AudioFile;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionServiceTest {

    @Mock
    private GroqClient groqClient;

    @Test
    void shouldTranscribeForcedLanguageTeluguByDefault() {
        GroqTranscriptionProperties properties = new GroqTranscriptionProperties(
                TranscriptionMode.FORCED_LANGUAGE, "te", List.of("te", "telugu", "en", "english")
        );
        TranscriptionService service = new TranscriptionService(groqClient, properties);

        AudioFile audioFile = new AudioFile("audio_bytes".getBytes(), "voice.ogg", "audio/ogg");
        when(groqClient.transcribeForcedLanguage(eq(audioFile), eq("te")))
                .thenReturn(Mono.just(new GroqTranscriptionResponse("నమస్కారం", null)));

        StepVerifier.create(service.transcribe(audioFile))
                .expectNext("నమస్కారం")
                .verifyComplete();

        verify(groqClient).transcribeForcedLanguage(audioFile, "te");
    }

    @Test
    void shouldTranscribeWhenDetectedLanguageIsInAllowedList() {
        GroqTranscriptionProperties properties = new GroqTranscriptionProperties(
                TranscriptionMode.FILTERED_LANGUAGES, "te", List.of("te", "telugu", "en", "english")
        );
        TranscriptionService service = new TranscriptionService(groqClient, properties);

        AudioFile audioFile = new AudioFile("audio_bytes".getBytes(), "voice.ogg", "audio/ogg");
        when(groqClient.transcribeVerbose(eq(audioFile)))
                .thenReturn(Mono.just(new GroqTranscriptionResponse("Hello there", "english")));

        StepVerifier.create(service.transcribe(audioFile))
                .expectNext("Hello there")
                .verifyComplete();
    }

    @Test
    void shouldIgnoreAndReturnEmptyWhenDetectedLanguageIsNotAllowed() {
        GroqTranscriptionProperties properties = new GroqTranscriptionProperties(
                TranscriptionMode.FILTERED_LANGUAGES, "te", List.of("te", "telugu", "en", "english")
        );
        TranscriptionService service = new TranscriptionService(groqClient, properties);

        AudioFile audioFile = new AudioFile("audio_bytes".getBytes(), "voice.ogg", "audio/ogg");
        when(groqClient.transcribeVerbose(eq(audioFile)))
                .thenReturn(Mono.just(new GroqTranscriptionResponse("नमस्ते", "hindi")));

        StepVerifier.create(service.transcribe(audioFile))
                .verifyComplete();
    }
}
