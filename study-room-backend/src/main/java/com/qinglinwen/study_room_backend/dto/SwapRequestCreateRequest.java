package com.qinglinwen.study_room_backend.dto;

import lombok.Data;

@Data
public class SwapRequestCreateRequest {
    private Long targetReservationId;
    private String message;
}
