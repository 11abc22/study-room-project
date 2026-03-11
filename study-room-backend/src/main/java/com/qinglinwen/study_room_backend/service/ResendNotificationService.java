package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.SwapRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "app.notification.resend", name = "enabled", havingValue = "true")
public class ResendNotificationService implements NotificationService {

    private final SwapRequestNotificationComposer composer;
    private final MailDeliveryService mailDeliveryService;

    public ResendNotificationService(SwapRequestNotificationComposer composer,
                                     MailDeliveryService mailDeliveryService) {
        this.composer = composer;
        this.mailDeliveryService = mailDeliveryService;
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
        mailDeliveryService.sendTextEmail(payload.getTargetEmail(), subject, text);
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
        mailDeliveryService.sendTextEmail(payload.getRequesterEmail(), subject, text);
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
        mailDeliveryService.sendTextEmail(payload.getRequesterEmail(), subject, text);
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
        mailDeliveryService.sendTextEmail(payload.getRequesterEmail(), subject, text);
    }
}
