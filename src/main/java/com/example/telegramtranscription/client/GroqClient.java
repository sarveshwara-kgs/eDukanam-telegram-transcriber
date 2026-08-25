package com.example.telegramtranscription.client;

import com.example.telegramtranscription.config.GroqProperties;
import com.example.telegramtranscription.dto.groq.GroqTranscriptionResponse;
import com.example.telegramtranscription.exception.GroqApiException;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Thin wrapper around Groq's OpenAI-compatible audio transcription endpoint.
 * https://console.groq.com/docs/speech-to-text
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);
    private static final String TRANSCRIPTIONS_PATH = "/audio/transcriptions";

    private final WebClient groqWebClient;
    private final GroqProperties groqProperties;

    public GroqClient(WebClient groqWebClient, GroqProperties groqProperties) {
        this.groqWebClient = groqWebClient;
        this.groqProperties = groqProperties;
    }

    /**
     * Sends audio bytes as multipart/form-data to Groq for transcription.
     */
    public Mono<GroqTranscriptionResponse> transcribe(AudioFile audioFile) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();

        builder.part("file", new ByteArrayResource(audioFile.bytes()) {
                    @Override
                    public String getFilename() {
                        return audioFile.filename();
                    }
                })
                .filename(audioFile.filename());

        builder.part("model", groqProperties.model());
        builder.part("response_format", "json");

        return groqWebClient.post()
                .uri(TRANSCRIPTIONS_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(GroqTranscriptionResponse.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Groq transcription failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof GroqApiException),
                        ex -> new GroqApiException("Failed to transcribe audio via Groq", ex));
    }
}
