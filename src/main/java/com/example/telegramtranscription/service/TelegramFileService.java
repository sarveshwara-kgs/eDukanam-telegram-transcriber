package com.example.telegramtranscription.service;

import com.example.telegramtranscription.client.TelegramClient;
import com.example.telegramtranscription.dto.telegram.TelegramFileResponse;
import com.example.telegramtranscription.exception.TelegramApiException;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Resolves a Telegram file_id into downloaded audio bytes, ready to be
 * handed off to the transcription service.
 */
@Service
public class TelegramFileService {

    private static final Logger log = LoggerFactory.getLogger(TelegramFileService.class);

    private final TelegramClient telegramClient;

    public TelegramFileService(TelegramClient telegramClient) {
        this.telegramClient = telegramClient;
    }

    /**
     * Given a Telegram file_id, resolves its file_path via getFile, then
     * downloads the raw bytes and wraps them in an AudioFile.
     *
     * @param fileId       Telegram file_id (from a Voice or Audio object)
     * @param mimeType     MIME type reported by Telegram (may be null)
     * @param fallbackName filename to use if Telegram's file_path has no usable name
     */
    public Mono<AudioFile> downloadAudio(String fileId, String mimeType, String fallbackName) {
        return telegramClient.getFile(fileId)
                .flatMap(this::validateFileResponse)
                .flatMap(fileResponse -> {
                    String filePath = fileResponse.result().filePath();
                    log.debug("Resolved Telegram file_id={} to file_path={}", fileId, filePath);
                    return telegramClient.downloadFile(filePath)
                            .map(bytes -> new AudioFile(bytes, resolveFilename(filePath, fallbackName), mimeType));
                });
    }

    /**
     * Downloads raw file bytes for an image or general file given its Telegram file_id.
     */
    public Mono<byte[]> downloadFileBytes(String fileId) {
        return telegramClient.getFile(fileId)
                .flatMap(this::validateFileResponse)
                .flatMap(fileResponse -> {
                    String filePath = fileResponse.result().filePath();
                    log.debug("Resolved Telegram file_id={} to file_path={}", fileId, filePath);
                    return telegramClient.downloadFile(filePath);
                });
    }

    private Mono<TelegramFileResponse> validateFileResponse(TelegramFileResponse response) {
        if (response == null || !response.ok() || response.result() == null
                || response.result().filePath() == null) {
            return Mono.error(new TelegramApiException("Telegram getFile returned an invalid response"));
        }
        return Mono.just(response);
    }

    private String resolveFilename(String filePath, String fallbackName) {
        if (filePath != null && filePath.contains("/")) {
            return filePath.substring(filePath.lastIndexOf('/') + 1);
        }
        if (filePath != null && !filePath.isBlank()) {
            return filePath;
        }
        return fallbackName != null ? fallbackName : "audio.ogg";
    }
}
