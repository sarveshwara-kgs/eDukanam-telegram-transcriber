package com.example.telegramtranscription.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.concurrent.TimeUnit;

/**
 * Creates two separate, named WebClient beans:
 *  - telegramWebClient: talks to the Telegram Bot API
 *  - groqWebClient: talks to the Groq API (authenticated with Bearer token)
 *
 * Timeouts are generous because audio download/upload can take a few seconds.
 */
@Configuration
public class WebClientConfig {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_WRITE_TIMEOUT_SEC = 60;
    private static final int MAX_IN_MEMORY_SIZE = 25 * 1024 * 1024; // 25MB, matches Telegram file limit

    @Bean
    public WebClient telegramWebClient(TelegramProperties telegramProperties) {
        return WebClient.builder()
                .baseUrl(telegramProperties.apiBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }

    @Bean
    public WebClient groqWebClient(GroqProperties groqProperties) {
        return WebClient.builder()
                .baseUrl(groqProperties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + groqProperties.key())
                .clientConnector(new ReactorClientHttpConnector(buildHttpClient()))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }

    private HttpClient buildHttpClient() {
        return HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECT_TIMEOUT_MS)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(READ_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(READ_WRITE_TIMEOUT_SEC, TimeUnit.SECONDS)));
    }
}
