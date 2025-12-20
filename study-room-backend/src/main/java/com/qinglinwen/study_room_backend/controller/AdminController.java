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
@CrossOrigin(origins = "http://localhost:5173")
public class AdminController {

    private static final String USER_ID_HEADER = "X-User-Id";

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final SeatCommentRepository seatCommentRepository;
    private final SeatRepository seatRepository;
    private final StudyRoomRepository studyRoomRepository;

    public AdminController(UserRepository userRepository,
                          ReservationRepository reservationRepository,
                          SeatCommentRepository seatCommentRepository,
                          SeatRepository seatRepository,
                          StudyRoomRepository studyRoomRepository) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.seatCommentRepository = seatCommentRepository;
        this.seatRepository = seatRepository;
        this.studyRoomRepository = studyRoomRepository;
    }

    /**
     * 校验当前请求用户是否为管理员（用户名为 admin）
     */
    private Long checkAdmin(String userIdHeader) {
        Long userId = parseUserId(userIdHeader);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户不存在"));

        if (!"admin".equals(user.getUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无管理员权限");
        }

        return userId;
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "缺少 X-User-Id 请求头");
        }
        try {
            return Long.parseLong(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Id 格式无效");
        }
    }

    // ==================== 预约管理 ====================

    /**
     * 查看所有预约，支持按 userId、status、date 筛选
     */
    @GetMapping("/reservations")
    public List<ReservationVO> listReservations(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String date) {

        checkAdmin(userIdHeader);

        List<Reservation> reservations = fetchReservations(userId, status, date);
        return enrichReservations(reservations);
    }

    /**
     * 取消任意预约（将 status 设为 2，与用户取消保持一致）
     */
    @DeleteMapping("/reservations/{id}")
    @Transactional
    public Map<String, Object> deleteReservation(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {

        checkAdmin(userIdHeader);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在"));

        reservation.setStatus(2);
        reservationRepository.save(reservation);

        return Map.of("message", "预约已取消");
    }

    /**
     * 修改预约状态
     */
    @PutMapping("/reservations/{id}/status")
    @Transactional
    public Map<String, Object> updateReservationStatus(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id,
            @RequestBody Map<String, Integer> body) {

        checkAdmin(userIdHeader);

        Integer newStatus = body.get("status");
        if (newStatus == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少 status 参数");
        }

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "预约不存在"));

        reservation.setStatus(newStatus);
        reservationRepository.save(reservation);

        return Map.of("message", "状态已更新");
    }

    // ==================== 留言管理 ====================

    /**
     * 查看所有留言
     */
    @GetMapping("/comments")
    public List<SeatCommentVO> listComments(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader) {

        checkAdmin(userIdHeader);

        List<SeatComment> comments = seatCommentRepository.findAll();
        Map<Long, User> userMap = userRepository.findAllById(
                comments.stream().map(SeatComment::getUserId).distinct().toList()
        ).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        return comments.stream()
                .map(comment -> toVO(comment, userMap.get(comment.getUserId())))
                .toList();
    }

    /**
     * 删除任意留言
     */
    @DeleteMapping("/comments/{id}")
    @Transactional
    public Map<String, Object> deleteComment(
            @RequestHeader(value = USER_ID_HEADER, required = false) String userIdHeader,
            @PathVariable Long id) {

        checkAdmin(userIdHeader);

        SeatComment comment = seatCommentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "留言不存在"));

        seatCommentRepository.delete(comment);

        return Map.of("message", "留言已删除");
    }

    // ==================== 私有辅助方法 ====================

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
            ReservationVO vo = new ReservationVO();
            vo.setId(r.getId());
            vo.setUserId(r.getUserId());
            User user = userMap.get(r.getUserId());
            vo.setUsername(user != null ? user.getUsername() : "用户" + r.getUserId());
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

    private SeatCommentVO toVO(SeatComment comment, User user) {
        SeatCommentVO vo = new SeatCommentVO();
        vo.setId(comment.getId());
        vo.setSeatId(comment.getSeatId());
        vo.setSeatCode(seatRepository.findById(comment.getSeatId())
                .map(Seat::getSeatCode).orElse("未知座位"));
        vo.setUserId(comment.getUserId());
        vo.setUsername(user != null ? user.getUsername() : "用户" + comment.getUserId());
        vo.setContent(comment.getContent());
        vo.setCreatedAt(comment.getCreatedAt());
        return vo;
    }
}
