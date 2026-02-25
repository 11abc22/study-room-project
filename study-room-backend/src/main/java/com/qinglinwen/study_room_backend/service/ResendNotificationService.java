package com.qinglinwen.study_room_backend.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qinglinwen.study_room_backend.config.NotificationProperties;
import com.qinglinwen.study_room_backend.entity.SwapRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service("notificationServiceDelegate")
@Primary
@ConditionalOnProperty(prefix = "app.notification.resend", name = "enabled", havingValue = "true")
public class ResendNotificationService implements NotificationService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationProperties notificationProperties;
    private final SwapRequestNotificationComposer composer;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ResendNotificationService(NotificationProperties notificationProperties,
                                     SwapRequestNotificationComposer composer) {
        this.notificationProperties = notificationProperties;
        this.composer = composer;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public void notifySwapRequestCreated(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getTargetEmail())) {
            log.warn("Skip swap request created email because target email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        String subject = String.format("【自习室换位】%s 向你发起换位请求", payload.getRequesterUsername());
        String text = "你收到一条新的换位请求。\n\n"
                + composer.buildSummaryLines(payload)
                + "\n请尽快前往系统查看并处理该请求。";
        sendEmail(payload.getTargetEmail(), subject, text);
    }

    @Override
    public void notifySwapRequestApproved(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request approved email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        String subject = String.format("【自习室换位】%s 已同意你的换位请求", payload.getTargetUsername());
        String text = "你的换位请求已被同意，系统已完成双方座位互换。\n\n"
                + composer.buildSummaryLines(payload)
                + "\n请前往系统确认新的座位信息。";
        sendEmail(payload.getRequesterEmail(), subject, text);
    }

    @Override
    public void notifySwapRequestRejected(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request rejected email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        String subject = String.format("【自习室换位】%s 拒绝了你的换位请求", payload.getTargetUsername());
        String text = "你的换位请求已被拒绝，原有座位保持不变。\n\n"
                + composer.buildSummaryLines(payload)
                + "\n你可以前往系统查看详情。";
        sendEmail(payload.getRequesterEmail(), subject, text);
    }

    @Override
    public void notifySwapRequestExpired(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request expired email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        String subject = String.format("【自习室换位】你发给 %s 的换位请求已过期", payload.getTargetUsername());
        String text = "你的换位请求因超时未处理而已过期，原有座位保持不变。\n\n"
                + composer.buildSummaryLines(payload)
                + "\n如仍需换位，请重新发起请求。";
        sendEmail(payload.getRequesterEmail(), subject, text);
    }

    private void sendEmail(String to, String subject, String text) {
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
