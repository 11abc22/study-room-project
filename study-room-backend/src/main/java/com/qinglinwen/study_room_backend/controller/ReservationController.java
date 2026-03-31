package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.dto.ReservationRequest;
import com.qinglinwen.study_room_backend.service.ReservationService;
import com.qinglinwen.study_room_backend.vo.ReservationVO;
import com.qinglinwen.study_room_backend.vo.SeatStatusVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    // 临时固定当前用户为 userId = 1
    private Long getCurrentUserId() {
        return 1L;
    }

    @PostMapping("/seat-status/{roomId}")
    public List<SeatStatusVO> getSeatStatus(@PathVariable Long roomId,
                                            @RequestBody ReservationRequest req) {
        return reservationService.getSeatStatus(roomId, req);
    }

    @PostMapping
    public Map<String, Object> createReservation(@RequestBody ReservationRequest req) {
        Long id = reservationService.createReservation(getCurrentUserId(), req);
        return Map.of("message", "预约成功", "reservationId", id);
    }

    @GetMapping("/my")
    public List<ReservationVO> myReservations() {
        return reservationService.getMyReservations(getCurrentUserId());
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateReservation(@PathVariable Long id,
                                                 @RequestBody ReservationRequest req) {
        reservationService.updateReservation(getCurrentUserId(), id, req);
        return Map.of("message", "修改成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> cancelReservation(@PathVariable Long id) {
        reservationService.cancelReservation(getCurrentUserId(), id);
        return Map.of("message", "取消成功");
    }
}