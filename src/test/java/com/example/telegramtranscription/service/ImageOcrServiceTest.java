package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.OpenRouterOcrClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImageOcrServiceTest {

    @Mock
    private OpenRouterOcrClient ocrClient;

    private ImageOcrService imageOcrService;

    @BeforeEach
    void setUp() {
        imageOcrService = new ImageOcrService(ocrClient);
    }

    @Test
    void shouldExtractTextSuccessfully() {
        byte[] imageBytes = "dummy_image_bytes".getBytes();
        when(ocrClient.extractTextFromImage(eq(imageBytes), eq("image/jpeg"), any()))
                .thenReturn(Mono.just("Meeting notes:\n1. Buy groceries\n2. Call bank"));

        StepVerifier.create(imageOcrService.extractText(imageBytes, "image/jpeg"))
                .expectNext("Meeting notes:\n1. Buy groceries\n2. Call bank")
                .verifyComplete();

        verify(ocrClient).extractTextFromImage(eq(imageBytes), eq("image/jpeg"), any());
    }

    @Test
    void shouldReturnDefaultFallbackWhenTextIsEmpty() {
        byte[] imageBytes = "blank_image".getBytes();
        when(ocrClient.extractTextFromImage(eq(imageBytes), eq("image/png"), any()))
                .thenReturn(Mono.just(""));

        StepVerifier.create(imageOcrService.extractText(imageBytes, "image/png"))
                .expectNext("no text found to transcribe!")
                .verifyComplete();
    }

    @Test
    void shouldStripThinkingTagsIfModelOutputsThoughtProcess() {
        byte[] imageBytes = "image_with_thinking".getBytes();
        when(ocrClient.extractTextFromImage(eq(imageBytes), eq("image/jpeg"), any()))
                .thenReturn(Mono.just("<think>\nAnalyzing handwriting...\nDetected line 1\n</think>\nActual handwritten text"));

        StepVerifier.create(imageOcrService.extractText(imageBytes, "image/jpeg"))
                .expectNext("Actual handwritten text")
                .verifyComplete();
    }
}
