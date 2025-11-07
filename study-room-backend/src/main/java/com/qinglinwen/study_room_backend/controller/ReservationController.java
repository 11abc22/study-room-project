package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.dto.ReservationRequest;
import com.qinglinwen.study_room_backend.service.ReservationService;
import com.qinglinwen.study_room_backend.vo.ReservationVO;
import com.qinglinwen.study_room_backend.vo.SeatStatusVO;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "http://localhost:5173")
public class ReservationController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    private Long getCurrentUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-User-Id 请求头");
        }

        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id 格式无效");
        }
    }

    @PostMapping("/seat-status/{roomId}")
    public List<SeatStatusVO> getSeatStatus(@PathVariable Long roomId,
                                            @RequestBody ReservationRequest req) {
        return reservationService.getSeatStatus(roomId, req);
    }

    @PostMapping
    public Map<String, Object> createReservation(@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
                                                 @RequestBody ReservationRequest req) {
        Long id = reservationService.createReservation(getCurrentUserId(userIdHeader), req);
        return Map.of("message", "预约成功", "reservationId", id);
    }

    @GetMapping("/my")
    public List<ReservationVO> myReservations(@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader) {
        return reservationService.getMyReservations(getCurrentUserId(userIdHeader));
    }

    @PutMapping("/{id}")
    public Map<String, Object> updateReservation(@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
                                                 @PathVariable Long id,
                                                 @RequestBody ReservationRequest req) {
        reservationService.updateReservation(getCurrentUserId(userIdHeader), id, req);
        return Map.of("message", "修改成功");
    }

    @DeleteMapping("/{id}")
    public Map<String, Object> cancelReservation(@RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
                                                 @PathVariable Long id) {
        reservationService.cancelReservation(getCurrentUserId(userIdHeader), id);
        return Map.of("message", "取消成功");
    }
}
