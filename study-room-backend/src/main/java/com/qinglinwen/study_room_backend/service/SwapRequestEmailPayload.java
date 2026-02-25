package com.qinglinwen.study_room_backend.service;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Value
@Builder
public class SwapRequestEmailPayload {
    Long swapRequestId;
    String requesterUsername;
    String requesterEmail;
    String targetUsername;
    String targetEmail;
    LocalDate reserveDate;
    LocalTime startTime;
    LocalTime endTime;
    String requesterRoomName;
    String requesterSeatCode;
    String targetRoomName;
    String targetSeatCode;
    String message;
    LocalDateTime expireAt;
    String frontendLink;
}
