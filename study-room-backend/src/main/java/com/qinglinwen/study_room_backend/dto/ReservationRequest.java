package com.qinglinwen.study_room_backend.dto;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ReservationRequest {
    private Long roomId;
    private Long seatId;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
}