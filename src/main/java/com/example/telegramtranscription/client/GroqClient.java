package com.example.telegramtranscription.client;

import com.example.telegramtranscription.config.GroqProperties;
import com.example.telegramtranscription.dto.groq.GroqTranscriptionResponse;
import com.example.telegramtranscription.exception.GroqApiException;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
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
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final WebClient groqWebClient;
    private final GroqProperties groqProperties;

    public GroqClient(WebClient groqWebClient, GroqProperties groqProperties) {
        this.groqWebClient = groqWebClient;
        this.groqProperties = groqProperties;
    }

    /**
     * Sends an image as a base64 Data URL to Groq's Vision Chat Completion endpoint.
     */
    public Mono<String> extractTextFromImage(byte[] imageBytes, String mimeType, String prompt) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Mono.error(new GroqApiException("Cannot process empty image"));
        }

        String effectiveMimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";
        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + effectiveMimeType + ";base64," + base64Image;

        com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest request =
                new com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest(
                        groqProperties.visionModel(),
                        java.util.List.of(
                                new com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest.ChatMessage(
                                        "user",
                                        java.util.List.of(
                                                com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest.MessageContent.textContent(prompt),
                                                com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest.MessageContent.imageUrlContent(dataUrl)
                                        )
                                )
                        ),
                        2048,
                        0.1
                );

        return groqWebClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(com.example.telegramtranscription.dto.groq.GroqChatCompletionResponse.class)
                .map(response -> {
                    String content = response.firstContent();
                    return content != null ? content.trim() : "";
                })
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Groq vision completion failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof GroqApiException), ex -> {
                    if (ex instanceof WebClientResponseException wcre) {
                        return new GroqApiException("Failed to perform OCR via Groq Vision (status "
                                + wcre.getStatusCode() + "): " + wcre.getResponseBodyAsString(), ex);
                    }
                    return new GroqApiException("Failed to perform OCR via Groq Vision", ex);
                });
    }

    /**
     * Sends audio bytes as multipart/form-data to Groq with standard json response format.
     */
    public Mono<GroqTranscriptionResponse> transcribe(AudioFile audioFile) {
        return transcribe(audioFile, null, "json");
    }

    /**
     * Sends audio bytes to Groq forcing a specific language (e.g. "te" for Telugu).
     */
    public Mono<GroqTranscriptionResponse> transcribeForcedLanguage(AudioFile audioFile, String language) {
        return transcribe(audioFile, language, "json");
    }

    /**
     * Sends audio bytes to Groq with verbose_json response format to capture detected language metadata.
     */
    public Mono<GroqTranscriptionResponse> transcribeVerbose(AudioFile audioFile) {
        return transcribe(audioFile, null, "verbose_json");
    }

    private Mono<GroqTranscriptionResponse> transcribe(AudioFile audioFile, String language, String responseFormat) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        MediaType mediaType = resolveMediaType(audioFile);

        builder.part("file", new ByteArrayResource(audioFile.bytes()) {
                    @Override
                    public String getFilename() {
                        return audioFile.filename();
                    }
                })
                .filename(audioFile.filename())
                .contentType(mediaType);

        builder.part("model", groqProperties.model());
        builder.part("response_format", responseFormat != null ? responseFormat : "json");

        if (language != null && !language.isBlank()) {
            builder.part("language", language);
        }

        return groqWebClient.post()
                .uri(TRANSCRIPTIONS_PATH)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .retrieve()
                .bodyToMono(GroqTranscriptionResponse.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Groq transcription failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof GroqApiException), ex -> {
                    if (ex instanceof WebClientResponseException wcre) {
                        return new GroqApiException("Failed to transcribe audio via Groq (status "
                                + wcre.getStatusCode() + "): " + wcre.getResponseBodyAsString(), ex);
                    }
                    return new GroqApiException("Failed to transcribe audio via Groq", ex);
                });
    }

    private MediaType resolveMediaType(AudioFile audioFile) {
        if (audioFile.filename() != null && audioFile.filename().toLowerCase().endsWith(".ogg")) {
            return MediaType.parseMediaType("audio/ogg");
        }
        if (audioFile.mimeType() != null && !audioFile.mimeType().isBlank()) {
            try {
                return MediaType.parseMediaType(audioFile.mimeType());
            } catch (Exception ignored) {
                // fallback to filename based resolution
            }
        }
        return MediaTypeFactory.getMediaType(audioFile.filename())
                .orElse(MediaType.APPLICATION_OCTET_STREAM);
    }
}
