package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findByRoomId(Long roomId);
}