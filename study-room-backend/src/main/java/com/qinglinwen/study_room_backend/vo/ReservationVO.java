package com.qinglinwen.study_room_backend.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationVO {
    private Long id;
    private Long userId;
    private String username;
    private Long roomId;
    private String roomName;
    private Long seatId;
    private String seatCode;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Integer status;
    private String displayStatus;
    private Long swapRequestId;
    private String recordType;
    private Boolean virtual;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}