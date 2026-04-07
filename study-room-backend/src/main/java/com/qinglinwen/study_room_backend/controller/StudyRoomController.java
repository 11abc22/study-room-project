package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import com.qinglinwen.study_room_backend.service.ReservationService;
import com.qinglinwen.study_room_backend.vo.RoomTimelineHourVO;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class StudyRoomController {

    private final StudyRoomRepository studyRoomRepository;
    private final SeatRepository seatRepository;
    private final ReservationService reservationService;

    public StudyRoomController(StudyRoomRepository studyRoomRepository,
                               SeatRepository seatRepository,
                               ReservationService reservationService) {
        this.studyRoomRepository = studyRoomRepository;
        this.seatRepository = seatRepository;
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<StudyRoom> listRooms() {
        return studyRoomRepository.findByStatus(1);
    }

    @GetMapping("/{roomId}/seats")
    public List<Seat> listSeats(@PathVariable Long roomId) {
        return seatRepository.findByRoomId(roomId);
    }

    @GetMapping("/{roomId}/timeline")
    public List<RoomTimelineHourVO> getRoomTimeline(@PathVariable Long roomId,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return reservationService.getRoomTimeline(roomId, date);
    }
}