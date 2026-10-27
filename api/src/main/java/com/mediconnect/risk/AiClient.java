package com.mediconnect.risk;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.mediconnect.common.ApiException;
import com.mediconnect.common.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;

/**
 * HTTP client for the Python AI microservice. Every call has a connect and read
 * timeout, and failures become a 503 with a friendly message so the rest of the
 * platform keeps working when the AI service is down.
 */
@Slf4j
@Component
public class AiClient {

    private final RestClient rest;

    public AiClient(AppProperties props, RestClient.Builder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(props.aiService().timeoutSeconds()));
        this.rest = builder.baseUrl(props.aiService().baseUrl()).requestFactory(factory).build();
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DiabetesInput(String gender, int age, int hypertension, int heartDisease, String smokingHistory,
                                double bmi, double hba1cLevel, int bloodGlucoseLevel) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record DiabetesPrediction(double probability, String riskBand, String modelVersion,
                                     List<String> factors, String recommendation) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatTurn(String role, String content) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatRequest(String message, List<ChatTurn> history) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Source(String policy, Integer page, String snippet) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record ChatReply(String reply, List<Source> sources, String suggestedSpecialization, String mode) {
    }

    public DiabetesPrediction predictDiabetes(DiabetesInput input) {
        return call(() -> rest.post().uri("/predict/diabetes").body(input).retrieve().body(DiabetesPrediction.class));
    }

    public ChatReply chat(ChatRequest request) {
        return call(() -> rest.post().uri("/assistant/chat").body(request).retrieve().body(ChatReply.class));
    }

    private <T> T call(java.util.function.Supplier<T> request) {
        try {
            T result = request.get();
            if (result == null) {
                throw new RestClientException("Empty response");
            }
            return result;
        } catch (RestClientException e) {
            log.warn("AI service call failed: {}", e.getMessage());
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "The AI service is unavailable right now. Please try again in a minute.");
        }
    }
}
