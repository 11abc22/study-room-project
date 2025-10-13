package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.StudyRoom;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudyRoomRepository extends JpaRepository<StudyRoom, Long> {
    List<StudyRoom> findByStatus(Integer status);
}