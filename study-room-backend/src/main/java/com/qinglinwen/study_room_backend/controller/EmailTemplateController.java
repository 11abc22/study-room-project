package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.entity.EmailTemplate;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import com.qinglinwen.study_room_backend.service.EmailTemplateKey;
import com.qinglinwen.study_room_backend.service.EmailTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/email-templates")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class EmailTemplateController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;
    private final EmailTemplateService emailTemplateService;

    public EmailTemplateController(UserRepository userRepository,
                                   EmailTemplateService emailTemplateService) {
        this.userRepository = userRepository;
        this.emailTemplateService = emailTemplateService;
    }

    @GetMapping
    public Map<String, Object> listTemplates(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader) {
        requireAdmin(userIdHeader);

        List<Map<String, Object>> templates = emailTemplateService.getAllTemplatesEnsuringDefaults().stream()
                .map(this::toResponse)
                .toList();

        return Map.of(
                "templates", templates,
                "placeholders", emailTemplateService.getSupportedPlaceholders()
        );
    }

    @PutMapping("/{templateKey}")
    public Map<String, Object> updateTemplate(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable String templateKey,
            @RequestBody EmailTemplateSaveRequest request) {
        requireAdmin(userIdHeader);

        EmailTemplate saved = emailTemplateService.saveTemplate(parseKey(templateKey), request.getSubject(), request.getBody());
        return toResponse(saved);
    }

    @DeleteMapping("/{templateKey}")
    public Map<String, Object> resetTemplate(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable String templateKey) {
        requireAdmin(userIdHeader);

        EmailTemplate reset = emailTemplateService.resetTemplate(parseKey(templateKey));
        return toResponse(reset);
    }

    private User requireAdmin(String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        if (!"admin".equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }
        return user;
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id format");
        }
    }

    private EmailTemplateKey parseKey(String templateKey) {
        try {
            return EmailTemplateKey.valueOf(templateKey);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported template key");
        }
    }

    private Map<String, Object> toResponse(EmailTemplate template) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("templateKey", template.getTemplateKey());
        response.put("subject", template.getSubject());
        response.put("body", template.getBody());
        response.put("createdAt", template.getCreatedAt());
        response.put("updatedAt", template.getUpdatedAt());
        return response;
    }
}
