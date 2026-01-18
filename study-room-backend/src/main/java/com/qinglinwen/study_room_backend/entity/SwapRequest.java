package com.qinglinwen.study_room_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "swap_request", indexes = {
        @Index(name = "idx_swap_target_status", columnList = "target_reservation_id,status"),
        @Index(name = "idx_swap_requester_status", columnList = "requester_user_id,status"),
        @Index(name = "idx_swap_target_user_status", columnList = "target_user_id,status"),
        @Index(name = "idx_swap_status_expire", columnList = "status,expire_at"),
        @Index(name = "idx_swap_requester_reservation", columnList = "requester_reservation_id"),
        @Index(name = "idx_swap_target_reservation", columnList = "target_reservation_id")
})
public class SwapRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_user_id", nullable = false)
    private Long requesterUserId;

    @Column(name = "requester_reservation_id", nullable = false)
    private Long requesterReservationId;

    @Column(name = "target_reservation_id", nullable = false)
    private Long targetReservationId;

    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    @Column(name = "reserve_date", nullable = false)
    private LocalDate reserveDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private Integer status;

    @Column(length = 500)
    private String message;

    @Column(name = "expire_at", nullable = false)
    private LocalDateTime expireAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processed_by")
    private Long processedBy;

    @Column(name = "process_reason", length = 64)
    private String processReason;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
