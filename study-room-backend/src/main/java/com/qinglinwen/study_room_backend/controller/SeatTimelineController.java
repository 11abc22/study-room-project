package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.service.ReservationService;
import com.qinglinwen.study_room_backend.vo.SeatTimelineHourVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/seats")
@CrossOrigin(origins = "http://localhost:5173")
public class SeatTimelineController {

    private final ReservationService reservationService;

    public SeatTimelineController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @GetMapping("/{seatId}/timeline")
    public List<SeatTimelineHourVO> getSeatTimeline(@PathVariable Long seatId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getSeatTimeline(seatId, date);
    }
}
