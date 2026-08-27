# Telegram Voice-to-Text Bot (Spring Boot + Groq)

A single Spring Boot service that:
1. Receives Telegram updates via webhook.
2. Detects voice/audio messages.
3. Downloads the audio from Telegram.
4. Sends it to Groq's Whisper API for transcription.
5. Sends the transcribed text back to the user via the same bot.

No separate microservice — Telegram handling and Groq transcription both live in this one service, organized into clean layers.

## Architecture

```
com.example.telegramtranscription
├── TelegramTranscriptionApplication.java   # main entry point
├── config/
│   ├── TelegramProperties.java             # telegram.bot.* config binding
│   ├── GroqProperties.java                 # groq.api.* config binding
│   ├── PropertiesConfig.java               # enables @ConfigurationProperties scanning
│   └── WebClientConfig.java                # WebClient beans (telegram + groq)
├── controller/
│   ├── TelegramWebhookController.java      # POST /telegram/webhook
│   └── HealthController.java               # GET /health
├── dto/
│   ├── telegram/                           # Telegram API request/response models
│   └── groq/                               # Groq API response models
├── model/
│   └── AudioFile.java                      # internal in-memory audio representation
├── client/
│   ├── TelegramClient.java                 # raw HTTP calls to Telegram Bot API
│   └── GroqClient.java                     # raw HTTP calls to Groq API
├── service/
│   ├── TelegramFileService.java            # resolves + downloads Telegram files
│   ├── TelegramMessageService.java         # sends messages back to users
│   ├── TranscriptionService.java           # business logic around Groq transcription
│   └── VoiceMessageHandlerService.java     # orchestrates the end-to-end flow
└── exception/
    ├── TelegramApiException.java
    ├── GroqApiException.java
    └── GlobalExceptionHandler.java         # REST-level error responses
```

**Flow:** `TelegramWebhookController` → `VoiceMessageHandlerService` → (`TelegramFileService` → `TranscriptionService` → `TelegramMessageService`).

This separation means you can later add new message types, commands, or business logic (e.g. persistence, summarization, translation) by adding new services without touching the controller or existing services.

## Prerequisites

- Java 17+
- Maven 3.9+
- A Telegram bot token from [@BotFather](https://t.me/BotFather)
- A Groq API key from [console.groq.com/keys](https://console.groq.com/keys)
- A publicly reachable HTTPS URL for the webhook (e.g. via [ngrok](https://ngrok.com) in development, or a real domain in production) — Telegram requires HTTPS for webhooks.

## Setup

### 1. Configure environment variables

Copy `.env.example` to `.env` (or export the variables directly) and fill in your values:

```bash
cp .env.example .env
```

```
TELEGRAM_BOT_TOKEN=123456789:AAExampleTokenReplaceMe
TELEGRAM_WEBHOOK_SECRET=some-random-secret-string
GROQ_API_KEY=gsk_exampleReplaceMe
GROQ_MODEL=whisper-large-v3-turbo
SERVER_PORT=8080
```

Export them before running (or use a tool like `direnv`/your IDE's run config):

```bash
export $(grep -v '^#' .env | xargs)
```

### 2. Build

```bash
cd backend/telegram-transcription
mvn clean package
```

### 3. Run

```bash
mvn spring-boot:run
```

or

```bash
java -jar target/telegram-transcription-1.0.0.jar
```

The service starts on `http://localhost:8080` (or `$SERVER_PORT`).

### 4. Expose it publicly (development)

```bash
ngrok http 8080
```

Note the HTTPS forwarding URL, e.g. `https://abcd1234.ngrok-free.app`.

### 5. Register the webhook with Telegram

```bash
curl -X POST "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/setWebhook" \
  -H "Content-Type: application/json" \
  -d '{
        "url": "https://abcd1234.ngrok-free.app/telegram/webhook",
        "secret_token": "some-random-secret-string"
      }'
```

Verify it's set:

```bash
curl "https://api.telegram.org/bot<TELEGRAM_BOT_TOKEN>/getWebhookInfo"
```

### 6. Test it

Open a chat with your bot in Telegram and send a voice message. Within a few seconds you should receive the transcribed text back.

## API Endpoints

| Method | Path                | Description                                  |
|--------|---------------------|-----------------------------------------------|
| POST   | `/telegram/webhook` | Receives updates from Telegram                |
| GET    | `/health`           | Basic health check                             |

## Configuration reference (`application.yml`)

| Property                         | Env var                       | Default                               | Description                                         |
|----------------------------------|-------------------------------|---------------------------------------|-----------------------------------------------------|
| `telegram.bot.token`             | `TELEGRAM_BOT_TOKEN`          | *(required)*                          | Bot token from BotFather                             |
| `telegram.bot.webhook-secret`    | `TELEGRAM_WEBHOOK_SECRET`     | *(empty = validation skipped)*        | Validates `X-Telegram-Bot-Api-Secret-Token` header   |
| `groq.api.key`                   | `GROQ_API_KEY`                | *(required)*                          | Groq API key                                         |
| `groq.api.model`                 | `GROQ_MODEL`                  | `whisper-large-v3-turbo`              | Groq speech-to-text model                            |
| `groq.transcription.mode`        | `GROQ_TRANSCRIPTION_MODE`     | `FORCED_LANGUAGE`                     | Mode: `FORCED_LANGUAGE` or `FILTERED_LANGUAGES`      |
| `groq.transcription.language`    | `GROQ_TRANSCRIPTION_LANGUAGE` | `te`                                  | Target language when in `FORCED_LANGUAGE` mode       |
| `groq.transcription.allowed-languages` | `GROQ_ALLOWED_LANGUAGES`| `te,telugu,en,english`                | Comma-separated allowed langs in `FILTERED_LANGUAGES`|
| `server.port`                    | `SERVER_PORT`                 | `8080`                                | HTTP port                                            |

## Design notes

- **Multipart upload to Groq**: audio bytes are sent as `multipart/form-data` per Groq's OpenAI-compatible `/audio/transcriptions` endpoint. The service automatically normalizes Telegram's OGG/Opus files (`.oga`, `.opus`, or extensionless) to `.ogg` format, which is officially supported by Groq Whisper STT.
- **Both `Voice` and `Audio` message types** are supported — voice notes (recorded in-app) and regular audio file uploads.
- **Webhook secret validation** is optional but recommended in production to ensure requests genuinely originate from Telegram.
- **Extensibility**: to add new behavior (e.g., persisting transcripts, supporting bot commands, translating text), add new services and call them from `VoiceMessageHandlerService`, or add new branches in `handleUpdate` — the controller and low-level clients never need to change.

## Extending

- **Add a database**: add `spring-boot-starter-data-jpa` + a datasource, then inject a repository into `VoiceMessageHandlerService` or a new service to persist transcripts per chat.
- **Add bot commands** (e.g. `/start`, `/help`): branch on `message.text()` in `VoiceMessageHandlerService.handleUpdate`.
- **Add other Groq features** (e.g. LLM summarization of the transcript): add a new client method in `GroqClient` and a new service, then call it after transcription completes.
