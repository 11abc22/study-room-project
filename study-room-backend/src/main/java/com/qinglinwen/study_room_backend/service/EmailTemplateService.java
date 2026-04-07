package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.EmailTemplate;
import com.qinglinwen.study_room_backend.repository.EmailTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class EmailTemplateService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final EmailTemplateRepository emailTemplateRepository;

    public EmailTemplateService(EmailTemplateRepository emailTemplateRepository) {
        this.emailTemplateRepository = emailTemplateRepository;
    }

    public List<EmailTemplate> getAllTemplatesEnsuringDefaults() {
        Arrays.stream(EmailTemplateKey.values()).forEach(this::ensureDefaultTemplateExists);
        return Arrays.stream(EmailTemplateKey.values())
                .map(this::getTemplateOrDefaultEntity)
                .collect(Collectors.toList());
    }

    public EmailTemplate getTemplateOrDefaultEntity(EmailTemplateKey key) {
        return emailTemplateRepository.findByTemplateKey(key.name())
                .orElseGet(() -> buildDefaultEntity(key));
    }

    public RenderedEmailTemplate renderTemplate(EmailTemplateKey key, Map<String, String> variables) {
        EmailTemplate template = getTemplateOrDefaultEntity(key);
        return new RenderedEmailTemplate(
                applyVariables(template.getSubject(), variables),
                applyVariables(template.getBody(), variables)
        );
    }

    @Transactional
    public EmailTemplate saveTemplate(EmailTemplateKey key, String subject, String body) {
        if (!StringUtils.hasText(subject) || !StringUtils.hasText(body)) {
            throw new IllegalArgumentException("Subject and body are required");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailTemplate template = emailTemplateRepository.findByTemplateKey(key.name())
                .orElseGet(EmailTemplate::new);
        template.setTemplateKey(key.name());
        template.setSubject(subject.trim());
        template.setBody(body);
        if (template.getCreatedAt() == null) {
            template.setCreatedAt(now);
        }
        template.setUpdatedAt(now);
        return emailTemplateRepository.save(template);
    }

    @Transactional
    public EmailTemplate resetTemplate(EmailTemplateKey key) {
        emailTemplateRepository.findByTemplateKey(key.name()).ifPresent(emailTemplateRepository::delete);
        return buildDefaultEntity(key);
    }

    public Map<String, String> getSupportedPlaceholders() {
        LinkedHashMap<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("{{requesterUsername}}", "Username of the user who initiated the swap request");
        placeholders.put("{{targetUsername}}", "Username of the user who received the swap request");
        placeholders.put("{{reserveDate}}", "Reservation date in yyyy-MM-dd format");
        placeholders.put("{{startTime}}", "Reservation start time in HH:mm format");
        placeholders.put("{{endTime}}", "Reservation end time in HH:mm format");
        placeholders.put("{{requesterRoomName}}", "Current room name of the requester");
        placeholders.put("{{requesterSeatCode}}", "Current seat code of the requester");
        placeholders.put("{{targetRoomName}}", "Current room name of the target user");
        placeholders.put("{{targetSeatCode}}", "Current seat code of the target user");
        placeholders.put("{{message}}", "Swap request message left by the requester");
        placeholders.put("{{expireAt}}", "Expiration timestamp in yyyy-MM-dd HH:mm format");
        placeholders.put("{{frontendLink}}", "Frontend page link for reviewing the request");
        placeholders.put("{{adminUsername}}", "Admin username for the admin test email template");
        placeholders.put("{{adminEmail}}", "Admin email for the admin test email template");
        placeholders.put("{{sentAt}}", "Send time for the admin test email template");
        return placeholders;
    }

    public Map<String, String> buildVariables(SwapRequestEmailPayload payload) {
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        variables.put("requesterUsername", fallback(payload.getRequesterUsername()));
        variables.put("targetUsername", fallback(payload.getTargetUsername()));
        variables.put("reserveDate", payload.getReserveDate() != null ? payload.getReserveDate().format(DATE_FORMATTER) : "-");
        variables.put("startTime", payload.getStartTime() != null ? payload.getStartTime().format(TIME_FORMATTER) : "-");
        variables.put("endTime", payload.getEndTime() != null ? payload.getEndTime().format(TIME_FORMATTER) : "-");
        variables.put("requesterRoomName", fallback(payload.getRequesterRoomName()));
        variables.put("requesterSeatCode", fallback(payload.getRequesterSeatCode()));
        variables.put("targetRoomName", fallback(payload.getTargetRoomName()));
        variables.put("targetSeatCode", fallback(payload.getTargetSeatCode()));
        variables.put("message", StringUtils.hasText(payload.getMessage()) ? payload.getMessage() : "-");
        variables.put("expireAt", payload.getExpireAt() != null ? payload.getExpireAt().format(DATETIME_FORMATTER) : "-");
        variables.put("frontendLink", fallback(payload.getFrontendLink()));
        return variables;
    }

    public Map<String, String> buildAdminTestVariables(String adminUsername, String adminEmail, LocalDateTime sentAt) {
        LinkedHashMap<String, String> variables = new LinkedHashMap<>();
        variables.put("adminUsername", fallback(adminUsername));
        variables.put("adminEmail", fallback(adminEmail));
        variables.put("sentAt", sentAt != null ? sentAt.format(DATETIME_FORMATTER) : "-");
        return variables;
    }

    private void ensureDefaultTemplateExists(EmailTemplateKey key) {
        if (emailTemplateRepository.findByTemplateKey(key.name()).isEmpty()) {
            EmailTemplate template = buildDefaultEntity(key);
            LocalDateTime now = LocalDateTime.now();
            template.setCreatedAt(now);
            template.setUpdatedAt(now);
            emailTemplateRepository.save(template);
        }
    }

    private String applyVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue() != null ? entry.getValue() : "");
        }
        return result;
    }

    private EmailTemplate buildDefaultEntity(EmailTemplateKey key) {
        EmailTemplate template = new EmailTemplate();
        template.setTemplateKey(key.name());
        template.setSubject(defaultSubject(key));
        template.setBody(defaultBody(key));
        template.setCreatedAt(LocalDateTime.now());
        template.setUpdatedAt(LocalDateTime.now());
        return template;
    }

    private String defaultSubject(EmailTemplateKey key) {
        return switch (key) {
            case SWAP_REQUEST_CREATED -> "[Study Room Swap] {{requesterUsername}} sent you a swap request";
            case SWAP_REQUEST_APPROVED -> "[Study Room Swap] {{targetUsername}} approved your swap request";
            case SWAP_REQUEST_REJECTED -> "[Study Room Swap] {{targetUsername}} rejected your swap request";
            case SWAP_REQUEST_EXPIRED -> "[Study Room Swap] Your request to {{targetUsername}} has expired";
            case ADMIN_TEST -> "[Study Room] Admin test email";
        };
    }

    private String defaultBody(EmailTemplateKey key) {
        return switch (key) {
            case SWAP_REQUEST_CREATED -> "Hello {{targetUsername}},\n\nYou have received a new seat swap request from {{requesterUsername}}.\n\nDate: {{reserveDate}}\nTime: {{startTime}} - {{endTime}}\nRequester seat: {{requesterRoomName}} / {{requesterSeatCode}}\nYour seat: {{targetRoomName}} / {{targetSeatCode}}\nMessage: {{message}}\nExpires at: {{expireAt}}\nLink: {{frontendLink}}\n\nPlease review the request in the system as soon as possible.";
            case SWAP_REQUEST_APPROVED -> "Hello {{requesterUsername}},\n\nYour seat swap request has been approved by {{targetUsername}}.\n\nDate: {{reserveDate}}\nTime: {{startTime}} - {{endTime}}\nYour original seat: {{requesterRoomName}} / {{requesterSeatCode}}\nApproved user's original seat: {{targetRoomName}} / {{targetSeatCode}}\nMessage: {{message}}\nLink: {{frontendLink}}\n\nThe system has completed the seat exchange. Please check your updated reservation details.";
            case SWAP_REQUEST_REJECTED -> "Hello {{requesterUsername}},\n\nYour seat swap request has been rejected by {{targetUsername}}.\n\nDate: {{reserveDate}}\nTime: {{startTime}} - {{endTime}}\nYour seat: {{requesterRoomName}} / {{requesterSeatCode}}\nTarget seat: {{targetRoomName}} / {{targetSeatCode}}\nMessage: {{message}}\nLink: {{frontendLink}}\n\nYour reservation remains unchanged.";
            case SWAP_REQUEST_EXPIRED -> "Hello {{requesterUsername}},\n\nYour seat swap request to {{targetUsername}} has expired because it was not processed in time.\n\nDate: {{reserveDate}}\nTime: {{startTime}} - {{endTime}}\nYour seat: {{requesterRoomName}} / {{requesterSeatCode}}\nTarget seat: {{targetRoomName}} / {{targetSeatCode}}\nMessage: {{message}}\nExpires at: {{expireAt}}\nLink: {{frontendLink}}\n\nIf you still want to swap seats, please create a new request.";
            case ADMIN_TEST -> "This is a test notification sent from the admin notifications page.\n\nAdmin username: {{adminUsername}}\nAdmin email: {{adminEmail}}\nSent at: {{sentAt}}\n\nIf you received this email, your Resend notification setup is working.";
        };
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
