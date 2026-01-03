package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    @Query("""
        select r from Reservation r
        where r.seatId = ?1
          and r.reserveDate = ?2
          and r.status = 1
          and r.startTime < ?4
          and r.endTime > ?3
    """)
    List<Reservation> findSeatConflicts(Long seatId, LocalDate reserveDate, LocalTime startTime, LocalTime endTime);

    @Query("""
        select r from Reservation r
        where r.userId = ?1
          and r.reserveDate = ?2
          and r.status = 1
          and r.startTime < ?4
          and r.endTime > ?3
    """)
    List<Reservation> findUserConflicts(Long userId, LocalDate reserveDate, LocalTime startTime, LocalTime endTime);

    @Query("""
        select r from Reservation r
        where r.roomId = ?1
          and r.reserveDate = ?2
          and r.status = 1
          and r.startTime < ?4
          and r.endTime > ?3
    """)
    List<Reservation> findRoomReservations(Long roomId, LocalDate reserveDate, LocalTime startTime, LocalTime endTime);

    List<Reservation> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Reservation> findByUserId(Long userId);

    List<Reservation> findByReserveDate(LocalDate reserveDate);

    List<Reservation> findByStatus(Integer status);

    List<Reservation> findByReserveDateAndStatus(LocalDate reserveDate, Integer status);

    List<Reservation> findByUserIdAndStatus(Long userId, Integer status);

    List<Reservation> findByRoomIdAndReserveDateAndStatus(Long roomId, LocalDate reserveDate, Integer status);

    List<Reservation> findBySeatIdAndReserveDateAndStatus(Long seatId, LocalDate reserveDate, Integer status);
}