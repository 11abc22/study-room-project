package com.qinglinwen.study_room_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class MailDeliveryService {

    private final ObjectProvider<ResendEmailSender> resendEmailSenderProvider;

    public MailDeliveryService(ObjectProvider<ResendEmailSender> resendEmailSenderProvider) {
        this.resendEmailSenderProvider = resendEmailSenderProvider;
    }

    public void sendTextEmail(String to, String subject, String text) {
        if (!StringUtils.hasText(to)) {
            log.warn("Skip email delivery because recipient is empty, subject={}", subject);
            return;
        }

        ResendEmailSender resendEmailSender = resendEmailSenderProvider.getIfAvailable();
        if (resendEmailSender == null) {
            log.info("Email delivery disabled. Would send email to={} subject={}", to, subject);
            return;
        }

        resendEmailSender.sendTextEmail(to, subject, text);
    }
}
