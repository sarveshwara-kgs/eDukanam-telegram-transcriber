package com.example.telegramtranscription.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Translates unhandled exceptions into clean JSON error responses instead
 * of leaking stack traces. Note: most business-logic errors during async
 * voice processing are already caught inside VoiceMessageHandlerService and
 * reported back to the Telegram user directly, so this mainly covers
 * request-parsing errors and truly unexpected failures at the controller level.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TelegramApiException.class)
    public ResponseEntity<Map<String, String>> handleTelegramApiException(TelegramApiException ex) {
        log.error("Telegram API error", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "telegram_api_error", "message", ex.getMessage()));
    }

    @ExceptionHandler(GroqApiException.class)
    public ResponseEntity<Map<String, String>> handleGroqApiException(GroqApiException ex) {
        log.error("Groq API error", ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "groq_api_error", "message", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("Unhandled exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "internal_error", "message", "An unexpected error occurred"));
    }
}
