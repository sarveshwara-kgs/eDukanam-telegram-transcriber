package com.example.telegramtranscription.config;

/**
 * Strategy mode for audio transcription with Groq Whisper.
 */
public enum TranscriptionMode {
    /**
     * Forcibly transcribe audio using a single fixed language code (e.g. "te" for Telugu).
     */
    FORCED_LANGUAGE,

    /**
     * Let Whisper auto-detect language and filter out any audio not in the allowed languages set.
     */
    FILTERED_LANGUAGES
}
