package com.example.telegramtranscription.service;

import com.example.telegramtranscription.dto.telegram.TelegramChat;
import com.example.telegramtranscription.dto.telegram.TelegramMessage;
import com.example.telegramtranscription.dto.telegram.TelegramUpdate;
import com.example.telegramtranscription.dto.telegram.TelegramVoice;
import com.example.telegramtranscription.model.AudioFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoiceMessageHandlerServiceTest {

    @Mock
    private TelegramFileService telegramFileService;

    @Mock
    private TranscriptionService transcriptionService;

    @Mock
    private TelegramMessageService telegramMessageService;

    private VoiceMessageHandlerService service;

    @BeforeEach
    void setUp() {
        service = new VoiceMessageHandlerService(telegramFileService, transcriptionService, telegramMessageService);
    }

    @Test
    void shouldNormalizeOgaExtensionToOggWhenProcessingVoiceMessage() {
        Long chatId = 12345L;
        TelegramVoice voice = new TelegramVoice("file_id_123", "unique_123", 5, "audio/ogg", 1024L);
        TelegramMessage message = new TelegramMessage(1L, new TelegramChat(chatId, "private"), voice, null, null);
        TelegramUpdate update = new TelegramUpdate(100L, message);

        AudioFile downloadedAudio = new AudioFile("dummy".getBytes(), "voice_123.oga", "audio/ogg");
        when(telegramFileService.downloadAudio(eq("file_id_123"), eq("audio/ogg"), eq("voice.ogg")))
                .thenReturn(Mono.just(downloadedAudio));

        ArgumentCaptor<AudioFile> audioFileCaptor = ArgumentCaptor.forClass(AudioFile.class);
        when(transcriptionService.transcribe(audioFileCaptor.capture()))
                .thenReturn(Mono.just("Hello world"));

        when(telegramMessageService.sendText(chatId, "Hello world"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleUpdate(update))
                .verifyComplete();

        assertEquals("voice_123.ogg", audioFileCaptor.getValue().filename());
        verify(telegramMessageService).sendText(chatId, "Hello world");
    }

    @Test
    void shouldHandleExtensionlessVoiceMessageByAddingOgg() {
        Long chatId = 12345L;
        TelegramVoice voice = new TelegramVoice("file_id_123", "unique_123", 5, "audio/ogg", 1024L);
        TelegramMessage message = new TelegramMessage(1L, new TelegramChat(chatId, "private"), voice, null, null);
        TelegramUpdate update = new TelegramUpdate(100L, message);

        AudioFile downloadedAudio = new AudioFile("dummy".getBytes(), "voice_file", "audio/ogg");
        when(telegramFileService.downloadAudio(eq("file_id_123"), eq("audio/ogg"), eq("voice.ogg")))
                .thenReturn(Mono.just(downloadedAudio));

        ArgumentCaptor<AudioFile> audioFileCaptor = ArgumentCaptor.forClass(AudioFile.class);
        when(transcriptionService.transcribe(audioFileCaptor.capture()))
                .thenReturn(Mono.just("Transcribed text"));

        when(telegramMessageService.sendText(chatId, "Transcribed text"))
                .thenReturn(Mono.empty());

        StepVerifier.create(service.handleUpdate(update))
                .verifyComplete();

        assertEquals("voice_file.ogg", audioFileCaptor.getValue().filename());
    }
}
