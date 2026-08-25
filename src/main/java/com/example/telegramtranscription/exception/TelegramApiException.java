package com.example.telegramtranscription.exception;

/**
 * Thrown when a call to the Telegram Bot API fails (network error,
 * non-2xx response, or malformed payload).
 */
public class TelegramApiException extends RuntimeException {

    public TelegramApiException(String message) {
        super(message);
    }

    public TelegramApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
