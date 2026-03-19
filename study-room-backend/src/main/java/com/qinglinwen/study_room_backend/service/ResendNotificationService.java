package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.SwapRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.notification.resend", name = "enabled", havingValue = "true")
public class ResendNotificationService implements NotificationService {

    private final SwapRequestNotificationComposer composer;
    private final MailDeliveryService mailDeliveryService;
    private final EmailTemplateService emailTemplateService;

    public ResendNotificationService(SwapRequestNotificationComposer composer,
                                     MailDeliveryService mailDeliveryService,
                                     EmailTemplateService emailTemplateService) {
        this.composer = composer;
        this.mailDeliveryService = mailDeliveryService;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void notifySwapRequestCreated(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getTargetEmail())) {
            log.warn("Skip swap request created email because target email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        sendSwapEmail(payload.getTargetEmail(), EmailTemplateKey.SWAP_REQUEST_CREATED, payload);
    }

    @Override
    public void notifySwapRequestApproved(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request approved email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        sendSwapEmail(payload.getRequesterEmail(), EmailTemplateKey.SWAP_REQUEST_APPROVED, payload);
    }

    @Override
    public void notifySwapRequestRejected(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request rejected email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        sendSwapEmail(payload.getRequesterEmail(), EmailTemplateKey.SWAP_REQUEST_REJECTED, payload);
    }

    @Override
    public void notifySwapRequestExpired(SwapRequest swapRequest) {
        SwapRequestEmailPayload payload = composer.compose(swapRequest);
        if (!StringUtils.hasText(payload.getRequesterEmail())) {
            log.warn("Skip swap request expired email because requester email is missing, swapRequestId={}", swapRequest.getId());
            return;
        }
        sendSwapEmail(payload.getRequesterEmail(), EmailTemplateKey.SWAP_REQUEST_EXPIRED, payload);
    }

    private void sendSwapEmail(String toEmail, EmailTemplateKey templateKey, SwapRequestEmailPayload payload) {
        Map<String, String> variables = emailTemplateService.buildVariables(payload);
        RenderedEmailTemplate rendered = emailTemplateService.renderTemplate(templateKey, variables);
        mailDeliveryService.sendTextEmail(toEmail, rendered.getSubject(), rendered.getBody());
    }
}
