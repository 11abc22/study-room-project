package com.qinglinwen.study_room_backend.controller;

import com.qinglinwen.study_room_backend.entity.Reservation;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.SeatComment;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.ReservationRepository;
import com.qinglinwen.study_room_backend.repository.SeatCommentRepository;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import com.qinglinwen.study_room_backend.service.AdminNotificationService;
import com.qinglinwen.study_room_backend.service.ReservationService;
import com.qinglinwen.study_room_backend.service.ReservationStatus;
import com.qinglinwen.study_room_backend.vo.ReservationVO;
import com.qinglinwen.study_room_backend.vo.SeatCommentVO;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:5173", "http://127.0.0.1:5173", "http://localhost:5174", "http://127.0.0.1:5174"})
public class AdminController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final SeatCommentRepository seatCommentRepository;
    private final SeatRepository seatRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final ReservationService reservationService;
    private final AdminNotificationService adminNotificationService;

    public AdminController(UserRepository userRepository,
                           ReservationRepository reservationRepository,
                           SeatCommentRepository seatCommentRepository,
                           SeatRepository seatRepository,
                           StudyRoomRepository studyRoomRepository,
                           ReservationService reservationService,
                           AdminNotificationService adminNotificationService) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.seatCommentRepository = seatCommentRepository;
        this.seatRepository = seatRepository;
        this.studyRoomRepository = studyRoomRepository;
        this.reservationService = reservationService;
        this.adminNotificationService = adminNotificationService;
    }

    private User requireAdmin(String userIdHeader) {
        Long userId = parseUserId(userIdHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        if (!"admin".equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Admin access required");
        }

        return user;
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing X-User-Id header");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid X-User-Id format");
        }
    }

    @GetMapping("/reservations")
    public List<ReservationVO> listReservations(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String date) {

        requireAdmin(userIdHeader);

        List<Reservation> reservations = fetchReservations(userId, status, date);
        return enrichReservations(reservations);
    }

    @DeleteMapping("/reservations/{id}")
    @Transactional
    public Map<String, Object> deleteReservation(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {

        requireAdmin(userIdHeader);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        reservationService.ensureNoActiveSwapRequest(reservation.getId());

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);

        return Map.of("message", "Reservation cancelled successfully");
    }

    @PutMapping("/reservations/{id}/status")
    @Transactional
    public Map<String, Object> updateReservationStatus(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        requireAdmin(userIdHeader);

        Integer newStatus = body.get("status");
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing status parameter");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation not found"));

        reservationService.ensureNoActiveSwapRequest(reservation.getId());

        reservation.setStatus(newStatus);
        reservationRepository.save(reservation);

        return Map.of("message", "Status updated successfully");
    }

    @GetMapping("/comments")
    public List<SeatCommentVO> listComments(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader) {

        requireAdmin(userIdHeader);

        List<SeatComment> comments = seatCommentRepository.findAll();
        Map<Long, User> userMap = userRepository.findAllById(
                comments.stream().map(SeatComment::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .map(comment -> toVO(comment, userMap.get(comment.getUserId())))
                .toList();
    }

    @DeleteMapping("/comments/{id}")
    @Transactional
    public Map<String, Object> deleteComment(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {

        requireAdmin(userIdHeader);

        SeatComment comment = seatCommentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Comment not found"));

        seatCommentRepository.delete(comment);

        return Map.of("message", "Comment deleted successfully");
    }

    @PostMapping("/notifications/test-email")
    public Map<String, Object> sendTestEmail(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader) {

        User adminUser = requireAdmin(userIdHeader);
        adminNotificationService.sendAdminTestEmail(adminUser);

        return Map.of(
                "message", "Test email has been queued for delivery",
                "targetEmail", adminUser.getEmail()
        );
    }

    private List<Reservation> fetchReservations(Long userId, Integer status, String date) {
        if (userId != null && status != null && date != null) {
            LocalDate reserveDate = LocalDate.parse(date);
            return reservationRepository.findByReserveDateAndStatus(reserveDate, status)
                    .stream().filter(r -> r.getUserId().equals(userId)).toList();
        }

        if (userId != null && status != null) {
            return reservationRepository.findByUserIdAndStatus(userId, status);
        }

        if (userId != null && date != null) {
            LocalDate reserveDate = LocalDate.parse(date);
            return reservationRepository.findByReserveDate(reserveDate)
                    .stream().filter(r -> r.getUserId().equals(userId)).toList();
        }

        if (status != null && date != null) {
            LocalDate reserveDate = LocalDate.parse(date);
            return reservationRepository.findByReserveDateAndStatus(reserveDate, status);
        }

        if (userId != null) {
            return reservationRepository.findByUserId(userId);
        }

        if (status != null) {
            return reservationRepository.findByStatus(status);
        }

        if (date != null) {
            return reservationRepository.findByReserveDate(LocalDate.parse(date));
        }

        return reservationRepository.findAll();
    }

    private List<ReservationVO> enrichReservations(List<Reservation> reservations) {
        Map<Long, String> roomMap = studyRoomRepository.findAll().stream()
                .collect(Collectors.toMap(StudyRoom::getId, StudyRoom::getName));

        Map<Long, String> seatMap = seatRepository.findAll().stream()
                .collect(Collectors.toMap(Seat::getId, Seat::getSeatCode));

        Map<Long, User> userMap = userRepository.findAllById(
                reservations.stream().map(Reservation::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return reservations.stream().map(r -> {
            ReservationVO vo = reservationService.toReservationVO(r, roomMap, seatMap, r.getUserId());
            User user = userMap.get(r.getUserId());
            vo.setUsername(user != null ? user.getUsername() : "User " + r.getUserId());
            return vo;
        }).collect(Collectors.toList());
    }

    private SeatCommentVO toVO(SeatComment comment, User user) {
        SeatCommentVO vo = new SeatCommentVO();
        vo.setId(comment.getId());
        vo.setSeatId(comment.getSeatId());
        vo.setSeatCode(seatRepository.findById(comment.getSeatId())
                .map(Seat::getSeatCode).orElse("Unknown seat"));
        vo.setUserId(comment.getUserId());
        vo.setUsername(user != null ? user.getUsername() : "User " + comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }
}
