package com.example.telegramtranscription.exception;

/**
 * Thrown when a call to the Groq transcription API fails (network error,
 * non-2xx response, or malformed payload).
 */
public class GroqApiException extends RuntimeException {

    public GroqApiException(String message) {
        super(message);
    }

    public GroqApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
