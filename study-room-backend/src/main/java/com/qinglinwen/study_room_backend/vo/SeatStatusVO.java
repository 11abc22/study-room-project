package com.qinglinwen.study_room_backend.vo;

import lombok.Data;

@Data
public class SeatStatusVO {
    private Long seatId;
    private String seatCode;
    private Integer seatStatus;
    private Boolean reserved;
    private Integer x;
    private Integer y;
}