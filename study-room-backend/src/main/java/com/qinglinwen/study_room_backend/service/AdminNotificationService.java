package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
public class AdminNotificationService {

    private final MailDeliveryService mailDeliveryService;
    private final NotificationDispatchExecutor notificationDispatchExecutor;

    public AdminNotificationService(MailDeliveryService mailDeliveryService,
                                    NotificationDispatchExecutor notificationDispatchExecutor) {
        this.mailDeliveryService = mailDeliveryService;
        this.notificationDispatchExecutor = notificationDispatchExecutor;
    }

    public void sendAdminTestEmail(User adminUser) {
        if (adminUser == null || !StringUtils.hasText(adminUser.getEmail())) {
            throw new IllegalArgumentException("Admin email is not configured");
        }

        String subject = "【Study Room】Admin test email";
        String text = "This is a test notification sent from the admin panel.\n\n"
                + "Admin username: " + adminUser.getUsername() + "\n"
                + "Admin email: " + adminUser.getEmail() + "\n"
                + "Sent at: " + LocalDateTime.now() + "\n\n"
                + "If you received this email, your Resend notification setup is working.";

        notificationDispatchExecutor.execute(() -> {
            try {
                mailDeliveryService.sendTextEmail(adminUser.getEmail(), subject, text);
            } catch (Exception ex) {
                log.warn("Failed to send admin test email to {}", adminUser.getEmail(), ex);
            }
        });
    }
}
