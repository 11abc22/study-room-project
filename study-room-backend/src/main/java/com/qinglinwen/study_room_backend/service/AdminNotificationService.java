package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
public class AdminNotificationService {

    private final MailDeliveryService mailDeliveryService;
    private final NotificationDispatchExecutor notificationDispatchExecutor;
    private final EmailTemplateService emailTemplateService;

    public AdminNotificationService(MailDeliveryService mailDeliveryService,
                                    NotificationDispatchExecutor notificationDispatchExecutor,
                                    EmailTemplateService emailTemplateService) {
        this.mailDeliveryService = mailDeliveryService;
        this.notificationDispatchExecutor = notificationDispatchExecutor;
        this.emailTemplateService = emailTemplateService;
    }

    public void sendAdminTestEmail(User adminUser) {
        if (adminUser == null || !StringUtils.hasText(adminUser.getEmail())) {
            throw new IllegalArgumentException("Admin email is not configured");
        }

        LocalDateTime sentAt = LocalDateTime.now();
        Map<String, String> variables = emailTemplateService.buildAdminTestVariables(
                adminUser.getUsername(),
                adminUser.getEmail(),
                sentAt
        );
        RenderedEmailTemplate rendered = emailTemplateService.renderTemplate(EmailTemplateKey.ADMIN_TEST, variables);

        notificationDispatchExecutor.execute(() -> {
            try {
                mailDeliveryService.sendTextEmail(adminUser.getEmail(), rendered.getSubject(), rendered.getBody());
            } catch (Exception ex) {
                log.warn("Failed to send admin test email to {}", adminUser.getEmail(), ex);
            }
        });
    }
}
