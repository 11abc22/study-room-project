package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.config.NotificationProperties;
import com.qinglinwen.study_room_backend.entity.Reservation;
import com.qinglinwen.study_room_backend.entity.Seat;
import com.qinglinwen.study_room_backend.entity.StudyRoom;
import com.qinglinwen.study_room_backend.entity.SwapRequest;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.ReservationRepository;
import com.qinglinwen.study_room_backend.repository.SeatRepository;
import com.qinglinwen.study_room_backend.repository.StudyRoomRepository;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Component
public class SwapRequestNotificationComposer {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final SeatRepository seatRepository;
    private final StudyRoomRepository studyRoomRepository;
    private final NotificationProperties notificationProperties;

    public SwapRequestNotificationComposer(UserRepository userRepository,
                                           ReservationRepository reservationRepository,
                                           SeatRepository seatRepository,
                                           StudyRoomRepository studyRoomRepository,
                                           NotificationProperties notificationProperties) {
        this.userRepository = userRepository;
        this.reservationRepository = reservationRepository;
        this.seatRepository = seatRepository;
        this.studyRoomRepository = studyRoomRepository;
        this.notificationProperties = notificationProperties;
    }

    public SwapRequestEmailPayload compose(SwapRequest swapRequest) {
        User requester = userRepository.findById(swapRequest.getRequesterUserId()).orElse(null);
        User target = userRepository.findById(swapRequest.getTargetUserId()).orElse(null);
        Reservation requesterReservation = reservationRepository.findById(swapRequest.getRequesterReservationId()).orElse(null);
        Reservation targetReservation = reservationRepository.findById(swapRequest.getTargetReservationId()).orElse(null);

        return SwapRequestEmailPayload.builder()
                .swapRequestId(swapRequest.getId())
                .requesterUsername(resolveUsername(requester, swapRequest.getRequesterUserId()))
                .requesterEmail(requester != null ? requester.getEmail() : null)
                .targetUsername(resolveUsername(target, swapRequest.getTargetUserId()))
                .targetEmail(target != null ? target.getEmail() : null)
                .reserveDate(swapRequest.getReserveDate())
                .startTime(swapRequest.getStartTime())
                .endTime(swapRequest.getEndTime())
                .requesterRoomName(resolveRoomName(requesterReservation))
                .requesterSeatCode(resolveSeatCode(requesterReservation))
                .targetRoomName(resolveRoomName(targetReservation))
                .targetSeatCode(resolveSeatCode(targetReservation))
                .message(swapRequest.getMessage())
                .expireAt(swapRequest.getExpireAt())
                .frontendLink(buildFrontendLink(swapRequest.getId()))
                .build();
    }

    public String buildSummaryLines(SwapRequestEmailPayload payload) {
        StringBuilder builder = new StringBuilder();
        builder.append("请求方：").append(payload.getRequesterUsername()).append('\n');
        builder.append("目标方：").append(payload.getTargetUsername()).append('\n');
        builder.append("日期：").append(payload.getReserveDate() != null ? payload.getReserveDate().format(DATE_FORMATTER) : "-").append('\n');
        builder.append("时间段：")
                .append(payload.getStartTime() != null ? payload.getStartTime().format(TIME_FORMATTER) : "-")
                .append(" - ")
                .append(payload.getEndTime() != null ? payload.getEndTime().format(TIME_FORMATTER) : "-")
                .append('\n');
        builder.append("请求方当前座位：")
                .append(fallback(payload.getRequesterRoomName()))
                .append(" / ")
                .append(fallback(payload.getRequesterSeatCode()))
                .append('\n');
        builder.append("目标方当前座位：")
                .append(fallback(payload.getTargetRoomName()))
                .append(" / ")
                .append(fallback(payload.getTargetSeatCode()))
                .append('\n');
        builder.append("留言：").append(StringUtils.hasText(payload.getMessage()) ? payload.getMessage() : "无").append('\n');
        if (payload.getExpireAt() != null) {
            builder.append("过期时间：").append(payload.getExpireAt()).append('\n');
        }
        if (StringUtils.hasText(payload.getFrontendLink())) {
            builder.append("查看链接：").append(payload.getFrontendLink()).append('\n');
        }
        return builder.toString();
    }

    private String resolveUsername(User user, Long userId) {
        return user != null ? user.getUsername() : "User " + userId;
    }

    private String resolveSeatCode(Reservation reservation) {
        if (reservation == null || reservation.getSeatId() == null) {
            return null;
        }
        Optional<Seat> seat = seatRepository.findById(reservation.getSeatId());
        return seat.map(Seat::getSeatCode).orElse(null);
    }

    private String resolveRoomName(Reservation reservation) {
        if (reservation == null || reservation.getRoomId() == null) {
            return null;
        }
        Optional<StudyRoom> room = studyRoomRepository.findById(reservation.getRoomId());
        return room.map(StudyRoom::getName).orElse(null);
    }

    private String buildFrontendLink(Long swapRequestId) {
        String baseUrl = notificationProperties.getFrontendBaseUrl();
        if (!StringUtils.hasText(baseUrl) || swapRequestId == null) {
            return null;
        }

        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String path = notificationProperties.getResend().getSwapRequestPath();
        String normalizedPath = StringUtils.hasText(path)
                ? (path.startsWith("/") ? path : "/" + path)
                : "/swap-requests";
        String separator = normalizedPath.contains("?") ? "&" : "?";
        return normalizedBaseUrl + normalizedPath + separator + "swapRequestId=" + URLEncoder.encode(String.valueOf(swapRequestId), StandardCharsets.UTF_8);
    }

    private String fallback(String value) {
        return StringUtils.hasText(value) ? value : "-";
    }
}
