package com.qinglinwen.study_room_backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglinwen.study_room_backend.config.NotificationProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.notification.resend", name = "enabled", havingValue = "true")
public class ResendEmailSender {

    private final NotificationProperties notificationProperties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendEmailSender(NotificationProperties notificationProperties) {
        this.notificationProperties = notificationProperties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public void sendTextEmail(String to, String subject, String text) {
        NotificationProperties.Resend resend = notificationProperties.getResend();
        validateRequiredConfig(resend);

        String endpoint = normalizeApiBaseUrl(resend.getApiBaseUrl()) + "/emails";
        EmailRequest requestBody = new EmailRequest(
                buildFrom(resend),
                List.of(to),
                subject,
                text,
                StringUtils.hasText(resend.getReplyTo()) ? List.of(resend.getReplyTo()) : null
        );

        try {
            String body = objectMapper.writeValueAsString(requestBody);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(15))
                    .header("Authorization", "Bearer " + resend.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Resend responded with status " + response.statusCode() + ": " + response.body());
            }
            log.info("Resend email sent successfully to={} subject={}", to, subject);
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Failed to send email via Resend", ex);
        }
    }

    private void validateRequiredConfig(NotificationProperties.Resend resend) {
        if (!StringUtils.hasText(resend.getApiKey())) {
            throw new IllegalStateException("Missing app.notification.resend.api-key configuration");
        }
        if (!StringUtils.hasText(resend.getFromEmail())) {
            throw new IllegalStateException("Missing app.notification.resend.from-email configuration");
        }
    }

    private String buildFrom(NotificationProperties.Resend resend) {
        if (StringUtils.hasText(resend.getFromName())) {
            return resend.getFromName() + " <" + resend.getFromEmail() + ">";
        }
        return resend.getFromEmail();
    }

    private String normalizeApiBaseUrl(String apiBaseUrl) {
        if (!StringUtils.hasText(apiBaseUrl)) {
            return "https://api.resend.com";
        }
        return apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
    }

    private record EmailRequest(String from,
                                List<String> to,
                                String subject,
                                String text,
                                List<String> reply_to) {
    }
}
