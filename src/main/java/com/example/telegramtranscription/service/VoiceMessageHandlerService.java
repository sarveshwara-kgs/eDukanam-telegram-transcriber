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
            "Send me a voice message and I'll transcribe it for you.";
    private static final String PROCESSING_ERROR_TEXT =
            "Sorry, I couldn't transcribe that voice message. Please try again.";

    private final TelegramFileService telegramFileService;
    private final TranscriptionService transcriptionService;
    private final TelegramMessageService telegramMessageService;

    public VoiceMessageHandlerService(TelegramFileService telegramFileService,
                                       TranscriptionService transcriptionService,
                                       TelegramMessageService telegramMessageService) {
        this.telegramFileService = telegramFileService;
        this.transcriptionService = transcriptionService;
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

        // Text or other message types: reply with a short help message.
        return telegramMessageService.sendText(chatId, NO_VOICE_HELP_TEXT);
    }

    private Mono<Void> handleVoice(Long chatId, TelegramVoice voice) {
        log.info("Processing voice message from chat={}, duration={}s", chatId, voice.duration());

        return telegramFileService.downloadAudio(voice.fileId(), voice.mimeType(), "voice.oga")
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
                .flatMap(transcriptionService::transcribe)
                .flatMap(text -> telegramMessageService.sendText(chatId, text))
                .onErrorResume(ex -> {
                    log.error("Failed to process audio message for chat={}", chatId, ex);
                    return telegramMessageService.sendText(chatId, PROCESSING_ERROR_TEXT);
                });
    }

    /**
     * Telegram voice notes are OGG/Opus but the resolved file_path sometimes
     * lacks a clear extension. Groq relies on the filename extension to pick
     * the right decoder, so we normalize voice notes to ".oga".
     */
    private Mono<AudioFile> ensureExtension(AudioFile audioFile) {
        String name = audioFile.filename();
        if (name == null || !name.contains(".")) {
            name = (name == null ? "voice" : name) + ".oga";
            return Mono.just(new AudioFile(audioFile.bytes(), name, audioFile.mimeType()));
        }
        return Mono.just(audioFile);
    }
}
