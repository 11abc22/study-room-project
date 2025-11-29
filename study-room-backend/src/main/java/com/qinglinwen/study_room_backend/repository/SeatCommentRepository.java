package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.SeatComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatCommentRepository extends JpaRepository<SeatComment, Long> {
    List<SeatComment> findTop10BySeatIdOrderByCreatedAtDescIdDesc(Long seatId);

    long countBySeatId(Long seatId);

    List<SeatComment> findBySeatIdOrderByCreatedAtAscIdAsc(Long seatId);
}
