package com.mediconnect.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Jwt jwt,
        Cors cors,
        AiService aiService,
        Mail mail,
        boolean seedDemoData) {

    public record Jwt(String secret, long ttlMinutes, boolean secureCookie) {}

    public record Cors(List<String> allowedOrigins) {}

    public record AiService(String baseUrl, int timeoutSeconds) {}

    public record Mail(boolean enabled, String from) {}
}
