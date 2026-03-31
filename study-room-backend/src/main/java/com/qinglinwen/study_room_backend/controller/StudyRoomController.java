package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "http://localhost:5173")
public class StudyRoomController {

    private final StudyRoomRepository studyRoomRepository;
    private final SeatRepository seatRepository;

    public StudyRoomController(StudyRoomRepository studyRoomRepository, SeatRepository seatRepository) {
        this.studyRoomRepository = studyRoomRepository;
        this.seatRepository = seatRepository;
    }

    @GetMapping
    public List<StudyRoom> listRooms() {
        return studyRoomRepository.findByStatus(1);
    }

    @GetMapping("/{roomId}/seats")
    public List<Seat> listSeats(@PathVariable Long roomId) {
        return seatRepository.findByRoomId(roomId);
    }
}