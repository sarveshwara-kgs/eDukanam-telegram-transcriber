package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.GroqClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Service to extract handwritten or printed text from images using Groq's Vision capabilities.
 */
@Service
public class ImageOcrService {

    private static final Logger log = LoggerFactory.getLogger(ImageOcrService.class);

    private static final String DEFAULT_OCR_PROMPT =
            "Extract and transcribe all handwritten or printed text from this image faithfully. "
                    + "Maintain the original structure and formatting where possible. "
                    + "If there is no text present, reply with '(no text detected)'. "
                    + "Return only the transcribed text without extra conversational commentary.";

    private final GroqClient groqClient;

    public ImageOcrService(GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    /**
     * Extracts handwritten/printed text from the given image bytes.
     */
    public Mono<String> extractText(byte[] imageBytes, String mimeType) {
        return extractText(imageBytes, mimeType, DEFAULT_OCR_PROMPT);
    }

    /**
     * Extracts text with a custom prompt.
     */
    public Mono<String> extractText(byte[] imageBytes, String mimeType, String prompt) {
        return groqClient.extractTextFromImage(imageBytes, mimeType, prompt)
                .map(text -> (text == null || text.isBlank()) ? "(no text detected)" : text.trim())
                .doOnNext(text -> log.debug("OCR result length={}", text.length()));
    }
}
