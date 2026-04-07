package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.dto.SwapRequestCreateRequest;
import com.qinglinwen.study_room_backend.service.SwapRequestService;
import com.qinglinwen.study_room_backend.vo.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/swap-requests")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class SwapRequestController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final SwapRequestService swapRequestService;

    public SwapRequestController(SwapRequestService swapRequestService) {
        this.swapRequestService = swapRequestService;
    }

    private Long getCurrentUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id format");
        }
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> createSwapRequest(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @RequestBody SwapRequestCreateRequest request) {
        return swapRequestService.createSwapRequest(getCurrentUserId(userIdHeader), request);
    }

    @PutMapping("/{id}/withdraw")
    public ApiResponse<Void> withdraw(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {
        return swapRequestService.withdraw(getCurrentUserId(userIdHeader), id);
    }

    @PutMapping("/{id}/approve")
    public ApiResponse<Map<String, Object>> approve(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {
        return swapRequestService.approve(getCurrentUserId(userIdHeader), id);
    }

    @PutMapping("/{id}/reject")
    public ApiResponse<Void> reject(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {
        return swapRequestService.reject(getCurrentUserId(userIdHeader), id);
    }

    @GetMapping("/check")
    public ApiResponse<Map<String, Object>> check(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam Long reservationId) {
        return swapRequestService.check(getCurrentUserId(userIdHeader), reservationId);
    }

    @GetMapping("/my-pending")
    public ApiResponse<Map<String, Object>> myPending(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam Long reservationId) {
        return swapRequestService.getMyPending(getCurrentUserId(userIdHeader), reservationId);
    }
}
