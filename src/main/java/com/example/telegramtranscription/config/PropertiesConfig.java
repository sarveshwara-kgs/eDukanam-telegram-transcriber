package com.example.telegramtranscription.config;

import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

/**
 * Enables component scanning for @ConfigurationProperties classes
 * (TelegramProperties, GroqProperties).
 */
@Configuration
@ConfigurationPropertiesScan
public class PropertiesConfig {
}
