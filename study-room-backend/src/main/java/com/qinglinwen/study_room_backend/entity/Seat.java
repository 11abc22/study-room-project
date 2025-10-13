package com.qinglinwen.study_room_backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "seat")
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "seat_code")
    private String seatCode;

    private Integer x;
    private Integer y;
    private Integer status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
