package com.qinglinwen.study_room_backend.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SeatCommentVO {
    private Long id;
    private Long seatId;
    private Long userId;
    private String username;
    private String content;
    private LocalDateTime createdAt;
}
