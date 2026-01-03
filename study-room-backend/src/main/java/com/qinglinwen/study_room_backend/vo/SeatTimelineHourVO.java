package com.qinglinwen.study_room_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SeatTimelineHourVO {
    private Integer hour;
    private Boolean reserved;
    private Boolean available;
}
