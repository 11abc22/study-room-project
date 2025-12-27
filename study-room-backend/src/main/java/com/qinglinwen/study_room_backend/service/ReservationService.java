package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.dto.ReservationRequest;
import com.qinglinwen.study_room_backend.entity.Reservation;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.repository.ReservationRepository;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import com.qinglinwen.study_room_backend.vo.ReservationVO;
import com.qinglinwen.study_room_backend.vo.SeatStatusVO;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final StudyRoomRepository studyRoomRepository;

    public ReservationService(ReservationRepository reservationRepository,
                              SeatRepository seatRepository,
                              StudyRoomRepository studyRoomRepository) {
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.studyRoomRepository = studyRoomRepository;
    }

    public List<SeatStatusVO> getSeatStatus(Long roomId, ReservationRequest req) {
        List<Seat> seats = seatRepository.findByRoomId(roomId);
        List<Reservation> reservations = reservationRepository.findRoomReservations(
                roomId, req.getReserveDate(), req.getStartTime(), req.getEndTime()
        );

        Set<Long> reservedSeatIds = reservations.stream()
                .map(Reservation::getSeatId)
                .collect(Collectors.toSet());

        List<SeatStatusVO> result = new ArrayList<>();
        for (Seat seat : seats) {
            SeatStatusVO vo = new SeatStatusVO();
            vo.setSeatId(seat.getId());
            vo.setSeatCode(seat.getSeatCode());
            vo.setSeatStatus(seat.getStatus());
            vo.setReserved(reservedSeatIds.contains(seat.getId()));
            vo.setX(seat.getX());
            vo.setY(seat.getY());
            result.add(vo);
        }
        return result;
    }

    @Transactional
    public Long createReservation(Long userId, ReservationRequest req) {
        validateRequest(req);

        Seat seat = seatRepository.findById(req.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (!seat.getRoomId().equals(req.getRoomId())) {
            throw new RuntimeException("The selected seat does not belong to this study room");
        }

        if (seat.getStatus() != 1) {
            throw new RuntimeException("This seat is unavailable for reservation");
        }

        if (!reservationRepository.findSeatConflicts(
                req.getSeatId(), req.getReserveDate(), req.getStartTime(), req.getEndTime()
        ).isEmpty()) {
            throw new RuntimeException("This seat is already reserved for the selected time slot");
        }

        if (!reservationRepository.findUserConflicts(
                userId, req.getReserveDate(), req.getStartTime(), req.getEndTime()
        ).isEmpty()) {
            throw new RuntimeException("You already have another reservation during this time slot");
        }

        Reservation reservation = new Reservation();
        reservation.setUserId(userId);
        reservation.setRoomId(req.getRoomId());
        reservation.setSeatId(req.getSeatId());
        reservation.setReserveDate(req.getReserveDate());
        reservation.setStartTime(req.getStartTime());
        reservation.setEndTime(req.getEndTime());
        reservation.setStatus(1);

        reservationRepository.save(reservation);
        return reservation.getId();
    }

    public List<ReservationVO> getMyReservations(Long userId) {
        List<Reservation> list = reservationRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<Long, String> roomMap = studyRoomRepository.findAll().stream()
                .collect(Collectors.toMap(StudyRoom::getId, StudyRoom::getName));

        Map<Long, String> seatMap = seatRepository.findAll().stream()
                .collect(Collectors.toMap(Seat::getId, Seat::getSeatCode));

        return list.stream().map(r -> {
            ReservationVO vo = new ReservationVO();
            vo.setId(r.getId());
            vo.setRoomId(r.getRoomId());
            vo.setRoomName(roomMap.get(r.getRoomId()));
            vo.setSeatId(r.getSeatId());
            vo.setSeatCode(seatMap.get(r.getSeatId()));
            vo.setReserveDate(r.getReserveDate());
            vo.setStartTime(r.getStartTime());
            vo.setEndTime(r.getEndTime());
            vo.setStatus(r.getStatus());
            return vo;
        }).collect(Collectors.toList());
    }

    @Transactional
    public void cancelReservation(Long userId, Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!reservation.getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to cancel this reservation");
        }

        reservation.setStatus(2);
        reservationRepository.save(reservation);
    }

    @Transactional
    public void updateReservation(Long userId, Long reservationId, ReservationRequest req) {
        validateRequest(req);

        Reservation old = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reservation not found"));

        if (!old.getUserId().equals(userId)) {
            throw new RuntimeException("You do not have permission to update this reservation");
        }

        Seat seat = seatRepository.findById(req.getSeatId())
                .orElseThrow(() -> new RuntimeException("Seat not found"));

        if (!seat.getRoomId().equals(req.getRoomId())) {
            throw new RuntimeException("The selected seat does not belong to this study room");
        }

        if (seat.getStatus() != 1) {
            throw new RuntimeException("This seat is unavailable for reservation");
        }

        boolean seatConflict = reservationRepository.findSeatConflicts(
                req.getSeatId(), req.getReserveDate(), req.getStartTime(), req.getEndTime()
        ).stream().anyMatch(r -> !r.getId().equals(reservationId));

        if (seatConflict) {
            throw new RuntimeException("This seat is already reserved for the selected time slot");
        }

        boolean userConflict = reservationRepository.findUserConflicts(
                userId, req.getReserveDate(), req.getStartTime(), req.getEndTime()
        ).stream().anyMatch(r -> !r.getId().equals(reservationId));

        if (userConflict) {
            throw new RuntimeException("You already have another reservation during this time slot");
        }

        old.setRoomId(req.getRoomId());
        old.setSeatId(req.getSeatId());
        old.setReserveDate(req.getReserveDate());
        old.setStartTime(req.getStartTime());
        old.setEndTime(req.getEndTime());

        reservationRepository.save(old);
    }

    private void validateRequest(ReservationRequest req) {
        if (req.getRoomId() == null || req.getSeatId() == null || req.getReserveDate() == null
                || req.getStartTime() == null || req.getEndTime() == null) {
            throw new RuntimeException("Reservation details are incomplete");
        }

        if (!req.getStartTime().isBefore(req.getEndTime())) {
            throw new RuntimeException("Start time must be earlier than end time");
        }
    }
}
