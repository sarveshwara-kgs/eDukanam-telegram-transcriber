package com.example.telegramtranscription.model;

/**
 * In-memory representation of a downloaded audio file, ready to be
 * forwarded to the Groq transcription API.
 *
 * @param bytes    raw audio bytes
 * @param filename filename to present to Groq (extension matters for format detection)
 * @param mimeType original MIME type reported by Telegram, if any
 */
public record AudioFile(
        byte[] bytes,
        String filename,
        String mimeType
) {
}
