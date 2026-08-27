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

    private static final String NO_TEXT_FOUND_MESSAGE = "no text found to transcribe!";

    private static final String DEFAULT_OCR_PROMPT =
            "You are a strict OCR transcriber. Transcribe all handwritten or printed text from this image faithfully. "
                    + "Maintain the original structure and formatting where possible. "
                    + "DO NOT output any thought process, preamble, explanation, or conversational commentary. "
                    + "Output ONLY the direct extracted text. "
                    + "If no text is found or visible in the image, output exactly: " + NO_TEXT_FOUND_MESSAGE;

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
                .map(this::cleanResponse)
                .doOnNext(text -> log.debug("OCR result length={}", text.length()));
    }

    private String cleanResponse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            return NO_TEXT_FOUND_MESSAGE;
        }

        String cleaned = rawText.trim();

        // Strip <think>...</think> tags if present from reasoning/thinking models
        if (cleaned.contains("<think>") && cleaned.contains("</think>")) {
            int endIndex = cleaned.lastIndexOf("</think>") + "</think>".length();
            cleaned = cleaned.substring(endIndex).trim();
        } else if (cleaned.startsWith("<think>")) {
            int endTag = cleaned.indexOf("</think>");
            if (endTag != -1) {
                cleaned = cleaned.substring(endTag + 8).trim();
            }
        }

        if (cleaned.isBlank()) {
            return NO_TEXT_FOUND_MESSAGE;
        }

        return cleaned;
    }
}
