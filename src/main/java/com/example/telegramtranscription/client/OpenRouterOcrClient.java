package com.example.telegramtranscription.client;

import com.example.telegramtranscription.config.OcrProperties;
import com.example.telegramtranscription.dto.groq.GroqChatCompletionRequest;
import com.example.telegramtranscription.dto.groq.GroqChatCompletionResponse;
import com.example.telegramtranscription.exception.GroqApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;

/**
 * Client for OpenAI-compatible Vision / OCR endpoints (such as OpenRouter).
 */
@Component
public class OpenRouterOcrClient {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterOcrClient.class);
    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";

    private final WebClient ocrWebClient;
    private final OcrProperties ocrProperties;

    public OpenRouterOcrClient(@Qualifier("ocrWebClient") WebClient ocrWebClient,
                               OcrProperties ocrProperties) {
        this.ocrWebClient = ocrWebClient;
        this.ocrProperties = ocrProperties;
    }

    /**
     * Sends an image as a base64 Data URL to OpenRouter's multimodal chat completion endpoint.
     */
    public Mono<String> extractTextFromImage(byte[] imageBytes, String mimeType, String prompt) {
        if (imageBytes == null || imageBytes.length == 0) {
            return Mono.error(new GroqApiException("Cannot process empty image"));
        }

        String effectiveMimeType = (mimeType != null && !mimeType.isBlank()) ? mimeType : "image/jpeg";
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);
        String dataUrl = "data:" + effectiveMimeType + ";base64," + base64Image;

        GroqChatCompletionRequest request = new GroqChatCompletionRequest(
                ocrProperties.model(),
                List.of(
                        new GroqChatCompletionRequest.ChatMessage(
                                "user",
                                List.of(
                                        GroqChatCompletionRequest.MessageContent.textContent(prompt),
                                        GroqChatCompletionRequest.MessageContent.imageUrlContent(dataUrl)
                                )
                        )
                ),
                2048,
                0.0
        );

        return ocrWebClient.post()
                .uri(CHAT_COMPLETIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GroqChatCompletionResponse.class)
                .map(response -> {
                    String content = response.firstContent();
                    return content != null ? content.trim() : "";
                })
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("OCR vision completion failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof GroqApiException), ex -> {
                    if (ex instanceof WebClientResponseException wcre) {
                        return new GroqApiException("Failed to perform OCR via Vision API (status "
                                + wcre.getStatusCode() + "): " + wcre.getResponseBodyAsString(), ex);
                    }
                    return new GroqApiException("Failed to perform OCR via Vision API", ex);
                });
    }
}
