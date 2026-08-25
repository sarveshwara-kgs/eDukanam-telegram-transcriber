package com.example.telegramtranscription.controller;

import com.example.telegramtranscription.config.TelegramProperties;
import com.example.telegramtranscription.dto.telegram.TelegramUpdate;
import com.example.telegramtranscription.service.VoiceMessageHandlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.scheduler.Schedulers;

/**
 * Receives incoming updates from Telegram via webhook.
 *
 * Configure the webhook once with Telegram, e.g.:
 *   https://api.telegram.org/bot<TOKEN>/setWebhook?url=https://your-domain.com/telegram/webhook&secret_token=<SECRET>
 *
 * Telegram expects a fast 200 OK response; the actual processing (download +
 * transcription + reply) is dispatched asynchronously so the webhook call
 * itself returns immediately and Telegram never times out or retries.
 */
@RestController
@RequestMapping("/telegram")
public class TelegramWebhookController {

    private static final Logger log = LoggerFactory.getLogger(TelegramWebhookController.class);
    private static final String SECRET_HEADER = "X-Telegram-Bot-Api-Secret-Token";

    private final VoiceMessageHandlerService voiceMessageHandlerService;
    private final TelegramProperties telegramProperties;

    public TelegramWebhookController(VoiceMessageHandlerService voiceMessageHandlerService,
                                      TelegramProperties telegramProperties) {
        this.voiceMessageHandlerService = voiceMessageHandlerService;
        this.telegramProperties = telegramProperties;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> receiveUpdate(
            @RequestBody TelegramUpdate update,
            @RequestHeader(value = SECRET_HEADER, required = false) String secretToken) {

        if (!isSecretValid(secretToken)) {
            log.warn("Rejected webhook call with invalid secret token");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        log.debug("Received Telegram update_id={}", update.updateId());

        // Process asynchronously off the request thread so we can ack Telegram immediately.
        voiceMessageHandlerService.handleUpdate(update)
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        unused -> { /* no-op on success */ },
                        error -> log.error("Unhandled error processing update_id={}", update.updateId(), error)
                );

        return ResponseEntity.ok().build();
    }

    private boolean isSecretValid(String providedSecret) {
        String configuredSecret = telegramProperties.webhookSecret();
        if (configuredSecret == null || configuredSecret.isBlank()) {
            // No secret configured -> skip validation (useful for local/dev setups).
            return true;
        }
        return configuredSecret.equals(providedSecret);
    }
}
