package com.qinglinwen.study_room_backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.notification")
public class NotificationProperties {

    /**
     * Frontend base URL used to build a swap request detail link in emails.
     */
    private String frontendBaseUrl;

    private final Resend resend = new Resend();

    @Data
    public static class Resend {
        /**
         * Whether Resend email delivery is enabled.
         */
        private boolean enabled = false;

        /**
         * Resend API key.
         */
        private String apiKey;

        /**
         * Sender email, e.g. no-reply@yourdomain.com.
         */
        private String fromEmail;

        /**
         * Optional sender display name.
         */
        private String fromName;

        /**
         * Optional reply-to email.
         */
        private String replyTo;

        /**
         * Optional custom link path on the frontend.
         */
        private String swapRequestPath = "/swap-requests";

        /**
         * Resend API base URL.
         */
        private String apiBaseUrl = "https://api.resend.com";
    }
}
