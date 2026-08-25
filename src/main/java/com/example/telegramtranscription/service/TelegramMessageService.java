package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.TelegramClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Responsible for sending outgoing messages back to Telegram users.
 * Kept separate from TelegramFileService so responsibilities stay focused
 * (file retrieval vs. message dispatch), making future extension easier
 * (e.g. adding sendPhoto, sendDocument, inline keyboards, etc.).
 */
@Service
public class TelegramMessageService {

    private static final int TELEGRAM_MAX_MESSAGE_LENGTH = 4096;

    private final TelegramClient telegramClient;

    public TelegramMessageService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * Sends a text message to the given chat, splitting into multiple
     * messages if the text exceeds Telegram's max message length.
     */
    public Mono<Void> sendText(Long chatId, String text) {
        if (text == null || text.isBlank()) {
            return Mono.empty();
        }

        if (text.length() <= TELEGRAM_MAX_MESSAGE_LENGTH) {
            return telegramClient.sendMessage(chatId, text);
        }

        return Mono.defer(() -> {
            Mono<Void> chain = Mono.empty();
            for (int start = 0; start < text.length(); start += TELEGRAM_MAX_MESSAGE_LENGTH) {
                int end = Math.min(start + TELEGRAM_MAX_MESSAGE_LENGTH, text.length());
                String chunk = text.substring(start, end);
                chain = chain.then(telegramClient.sendMessage(chatId, chunk));
            }
            return chain;
        });
    }
}
