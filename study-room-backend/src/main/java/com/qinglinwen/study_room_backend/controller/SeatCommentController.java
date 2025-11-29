package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.dto.SeatCommentCreateRequest;
import com.qinglinwen.study_room_backend.service.SeatCommentService;
import com.qinglinwen.study_room_backend.vo.SeatCommentVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "http://localhost:5173")
public class SeatCommentController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final SeatCommentService seatCommentService;

    public SeatCommentController(SeatCommentService seatCommentService) {
        this.seatCommentService = seatCommentService;
    }

    @GetMapping("/{seatId}/comments")
    public List<SeatCommentVO> listComments(@PathVariable Long seatId) {
        return seatCommentService.listComments(seatId);
    }

    @PostMapping("/{seatId}/comments")
    public SeatCommentVO createComment(@PathVariable Long seatId,
                                       @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
                                       @RequestBody SeatCommentCreateRequest request) {
        return seatCommentService.createComment(seatId, getCurrentUserId(userIdHeader), request);
    }

    private Long getCurrentUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-User-Id 请求头");
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id 格式无效");
        }
    }
}
