package com.qinglinwen.study_room_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RoomTimelineHourVO {
    private Integer hour;
    private Integer occupied;
    private Integer total;
    private Integer available;
    private String status;
}
