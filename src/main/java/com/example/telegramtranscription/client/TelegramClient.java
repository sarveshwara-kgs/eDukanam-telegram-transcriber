package com.example.telegramtranscription.client;

import com.example.telegramtranscription.config.TelegramProperties;
import com.example.telegramtranscription.dto.telegram.TelegramFileResponse;
import com.example.telegramtranscription.exception.TelegramApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Thin wrapper around the raw Telegram Bot API HTTP endpoints.
 * Knows nothing about business logic (voice handling, transcription, etc.) -
 * only how to talk to Telegram.
 */
@Component
public class TelegramClient {

    private static final Logger log = LoggerFactory.getLogger(TelegramClient.class);

    private final WebClient telegramWebClient;
    private final TelegramProperties telegramProperties;

    public TelegramClient(WebClient telegramWebClient, TelegramProperties telegramProperties) {
        this.telegramWebClient = telegramWebClient;
        this.telegramProperties = telegramProperties;
    }

    /**
     * Calls Telegram's "getFile" to resolve a file_id into a downloadable file_path.
     * https://core.telegram.org/bots/api#getfile
     */
    public Mono<TelegramFileResponse> getFile(String fileId) {
        String uri = "/bot" + telegramProperties.token() + "/getFile";
        return telegramWebClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(uri)
                        .queryParam("file_id", fileId)
                        .build())
                .retrieve()
                .bodyToMono(TelegramFileResponse.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Telegram getFile failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof TelegramApiException),
                        ex -> new TelegramApiException("Failed to resolve file from Telegram", ex));
    }

    /**
     * Downloads the raw bytes of a file from Telegram's file server, given the
     * file_path returned by getFile.
     * https://core.telegram.org/bots/api#file
     */
    public Mono<byte[]> downloadFile(String filePath) {
        String uri = "/file/bot" + telegramProperties.token() + "/" + filePath;
        return telegramWebClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Telegram file download failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof TelegramApiException),
                        ex -> new TelegramApiException("Failed to download file from Telegram", ex));
    }

    /**
     * Sends a plain text message to a chat.
     * https://core.telegram.org/bots/api#sendmessage
     */
    public Mono<Void> sendMessage(Long chatId, String text) {
        String uri = "/bot" + telegramProperties.token() + "/sendMessage";
        return telegramWebClient.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "chat_id", chatId,
                        "text", text
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(WebClientResponseException.class, ex ->
                        log.error("Telegram sendMessage failed: status={}, body={}",
                                ex.getStatusCode(), ex.getResponseBodyAsString()))
                .onErrorMap(ex -> !(ex instanceof TelegramApiException),
                        ex -> new TelegramApiException("Failed to send message via Telegram", ex));
    }
}
