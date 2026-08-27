package com.example.telegramtranscription.service;

import com.example.telegramtranscription.dto.telegram.TelegramAudio;
import com.example.telegramtranscription.dto.telegram.TelegramMessage;
import com.example.telegramtranscription.dto.telegram.TelegramUpdate;
import com.example.telegramtranscription.dto.telegram.TelegramVoice;
import com.example.telegramtranscription.model.AudioFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Orchestrates the end-to-end flow for a single incoming Telegram update:
 *   1. Determine if the update contains a voice/audio message.
 *   2. Download the audio via TelegramFileService.
 *   3. Transcribe it via TranscriptionService (Groq).
 *   4. Send the resulting text back via TelegramMessageService.
 *
 * This is the main extension point for future business logic (e.g. saving
 * transcripts, routing commands, summarization, etc.) - new behavior can be
 * added here or in new sibling services without touching the controller.
 */
@Service
public class VoiceMessageHandlerService {

    private static final Logger log = LoggerFactory.getLogger(VoiceMessageHandlerService.class);

    private static final String NO_VOICE_HELP_TEXT =
            "Send me a voice message, audio file, or a photo with handwritten notes and I'll transcribe it for you.";
    private static final String PROCESSING_ERROR_TEXT =
            "Sorry, I couldn't process that message. Please try again.";

    private final TelegramFileService telegramFileService;
    private final TranscriptionService transcriptionService;
    private final ImageOcrService imageOcrService;
    private final TelegramMessageService telegramMessageService;

    public VoiceMessageHandlerService(TelegramFileService telegramFileService,
                                       TranscriptionService transcriptionService,
                                       ImageOcrService imageOcrService,
                                       TelegramMessageService telegramMessageService) {
        this.telegramFileService = telegramFileService;
        this.transcriptionService = transcriptionService;
        this.imageOcrService = imageOcrService;
        this.telegramMessageService = telegramMessageService;
    }

    /**
     * Entry point called by the webhook controller for every incoming update.
     */
    public Mono<Void> handleUpdate(TelegramUpdate update) {
        TelegramMessage message = update.message();
        if (message == null || message.chat() == null) {
            log.debug("Update {} has no message/chat, ignoring", update.updateId());
            return Mono.empty();
        }

        Long chatId = message.chat().id();

        if (message.voice() != null) {
            return handleVoice(chatId, message.voice());
        }

        if (message.audio() != null) {
            return handleAudio(chatId, message.audio());
        }

        if (message.photo() != null && !message.photo().isEmpty()) {
            return handlePhoto(chatId, message.photo());
        }

        // Text or other message types: reply with a short help message.
        return telegramMessageService.sendText(chatId, NO_VOICE_HELP_TEXT);
    }

    private Mono<Void> handlePhoto(Long chatId, java.util.List<com.example.telegramtranscription.dto.telegram.TelegramPhotoSize> photos) {
        // Telegram provides photos in ascending order of resolution; pick the last (highest quality) one
        com.example.telegramtranscription.dto.telegram.TelegramPhotoSize largestPhoto = photos.get(photos.size() - 1);
        log.info("Processing photo OCR for chat={}, width={}, height={}, fileSize={}",
                chatId, largestPhoto.width(), largestPhoto.height(), largestPhoto.fileSize());

        return telegramFileService.downloadFileBytes(largestPhoto.fileId())
                .flatMap(bytes -> imageOcrService.extractText(bytes, "image/jpeg"))
                .flatMap(text -> telegramMessageService.sendText(chatId, text))
                .onErrorResume(ex -> {
                    log.error("Failed to process photo OCR for chat={}", chatId, ex);
                    return telegramMessageService.sendText(chatId, PROCESSING_ERROR_TEXT);
                });
    }

    private Mono<Void> handleVoice(Long chatId, TelegramVoice voice) {
        log.info("Processing voice message from chat={}, duration={}s", chatId, voice.duration());

        return telegramFileService.downloadAudio(voice.fileId(), voice.mimeType(), "voice.ogg")
                .flatMap(this::ensureExtension)
                .flatMap(transcriptionService::transcribe)
                .flatMap(text -> telegramMessageService.sendText(chatId, text))
                .onErrorResume(ex -> {
                    log.error("Failed to process voice message for chat={}", chatId, ex);
                    return telegramMessageService.sendText(chatId, PROCESSING_ERROR_TEXT);
                });
    }

    private Mono<Void> handleAudio(Long chatId, TelegramAudio audio) {
        log.info("Processing audio message from chat={}, duration={}s", chatId, audio.duration());

        String fallbackName = audio.fileName() != null ? audio.fileName() : "audio.mp3";

        return telegramFileService.downloadAudio(audio.fileId(), audio.mimeType(), fallbackName)
                .flatMap(this::ensureExtension)
                .flatMap(transcriptionService::transcribe)
                .flatMap(text -> telegramMessageService.sendText(chatId, text))
                .onErrorResume(ex -> {
                    log.error("Failed to process audio message for chat={}", chatId, ex);
                    return telegramMessageService.sendText(chatId, PROCESSING_ERROR_TEXT);
                });
    }

    /**
     * Telegram voice notes are OGG/Opus with .oga or .opus extensions.
     * Groq Whisper STT strictly requires supported file extensions
     * (flac, mp3, mp4, mpeg, mpga, m4a, ogg, wav, webm), so we normalize
     * .oga, .opus, or extensionless voice notes to ".ogg".
     */
    private Mono<AudioFile> ensureExtension(AudioFile audioFile) {
        String name = audioFile.filename();
        if (name == null || name.isBlank()) {
            name = "audio.ogg";
        } else if (name.toLowerCase().endsWith(".oga")) {
            name = name.substring(0, name.length() - 4) + ".ogg";
        } else if (name.toLowerCase().endsWith(".opus")) {
            name = name.substring(0, name.length() - 5) + ".ogg";
        } else if (!name.contains(".")) {
            name = name + ".ogg";
        }
        return Mono.just(new AudioFile(audioFile.bytes(), name, audioFile.mimeType()));
    }
}
