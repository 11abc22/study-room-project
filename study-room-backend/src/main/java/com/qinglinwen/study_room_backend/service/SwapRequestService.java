package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.dto.SwapRequestCreateRequest;
import com.qinglinwen.study_room_backend.entity.Reservation;
import com.qinglinwen.study_room_backend.entity.SwapRequest;
import com.qinglinwen.study_room_backend.entity.User;
import com.qinglinwen.study_room_backend.repository.ReservationRepository;
import com.qinglinwen.study_room_backend.repository.SwapRequestRepository;
import com.qinglinwen.study_room_backend.repository.UserRepository;
import com.qinglinwen.study_room_backend.vo.ApiResponse;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
public class SwapRequestService {

    private final SwapRequestRepository swapRequestRepository;
    private final ReservationRepository reservationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public SwapRequestService(SwapRequestRepository swapRequestRepository,
                              ReservationRepository reservationRepository,
                              UserRepository userRepository,
                              NotificationService notificationService) {
        this.swapRequestRepository = swapRequestRepository;
        this.reservationRepository = reservationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public ApiResponse<Map<String, Object>> createSwapRequest(Long userId, SwapRequestCreateRequest request) {
        if (request.getTargetReservationId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing targetReservationId");
        }

        Reservation targetReservation = reservationRepository.findByIdForUpdate(request.getTargetReservationId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Reservation not found"));

        validateTargetReservation(targetReservation);

        if (targetReservation.getUserId().equals(userId)) {
            throw new ResponseStatusException(BAD_REQUEST, "Cannot request swap for your own reservation");
        }

        Reservation requesterReservation = findRequesterReservation(userId, targetReservation);

        if (!swapRequestRepository.findRequesterActiveForSlot(
                userId,
                targetReservation.getReserveDate(),
                targetReservation.getStartTime(),
                targetReservation.getEndTime(),
                SwapRequestStatus.ACTIVE_STATUSES).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "You already have an active swap request in this time slot");
        }

        if (swapRequestRepository.findFirstByTargetReservationIdAndStatusIn(
                targetReservation.getId(), SwapRequestStatus.ACTIVE_STATUSES).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "This reservation already has an active swap request");
        }

        if (!swapRequestRepository.findByRequesterUserIdAndTargetReservationIdAndStatus(
                userId, targetReservation.getId(), SwapRequestStatus.REJECTED).isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "You have already been rejected for this reservation");
        }

        SwapRequest swapRequest = new SwapRequest();
        swapRequest.setRequesterUserId(userId);
        swapRequest.setRequesterReservationId(requesterReservation.getId());
        swapRequest.setTargetReservationId(targetReservation.getId());
        swapRequest.setTargetUserId(targetReservation.getUserId());
        swapRequest.setReserveDate(targetReservation.getReserveDate());
        swapRequest.setStartTime(targetReservation.getStartTime());
        swapRequest.setEndTime(targetReservation.getEndTime());
        swapRequest.setStatus(SwapRequestStatus.PENDING);
        swapRequest.setMessage(normalizeMessage(request.getMessage()));
        swapRequest.setExpireAt(resolveExpireAt(targetReservation));

        swapRequestRepository.save(swapRequest);
        notificationService.notifySwapRequestCreated(swapRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("swapRequestId", swapRequest.getId());
        data.put("status", "REQUESTING");
        return ApiResponse.success("换位请求已发送", data);
    }

    @Transactional
    public ApiResponse<Void> withdraw(Long userId, Long swapRequestId) {
        SwapRequest swapRequest = getPendingSwapRequestForUpdate(swapRequestId);
        if (!swapRequest.getRequesterUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to withdraw this swap request");
        }
        markSwapRequestFinished(swapRequest, SwapRequestStatus.WITHDRAWN, userId, "WITHDRAWN");
        return ApiResponse.success("换位请求已撤回", null);
    }

    @Transactional
    public ApiResponse<Map<String, Object>> approve(Long userId, Long swapRequestId) {
        SwapRequest swapRequest = getPendingSwapRequestForUpdate(swapRequestId);
        if (!swapRequest.getTargetUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to approve this swap request");
        }

        Reservation requesterReservation = getLockedActiveReservation(swapRequest.getRequesterReservationId());
        Reservation targetReservation = getLockedActiveReservation(swapRequest.getTargetReservationId());
        validateAlignedReservations(requesterReservation, targetReservation);

        Long requesterOriginalSeatId = requesterReservation.getSeatId();
        Long requesterOriginalRoomId = requesterReservation.getRoomId();
        Long targetOriginalSeatId = targetReservation.getSeatId();
        Long targetOriginalRoomId = targetReservation.getRoomId();

        requesterReservation.setSeatId(targetOriginalSeatId);
        requesterReservation.setRoomId(targetOriginalRoomId);
        requesterReservation.setStatus(ReservationStatus.RESERVED);

        targetReservation.setSeatId(requesterOriginalSeatId);
        targetReservation.setRoomId(requesterOriginalRoomId);
        targetReservation.setStatus(ReservationStatus.RESERVED);

        reservationRepository.save(requesterReservation);
        reservationRepository.save(targetReservation);
        markSwapRequestFinished(swapRequest, SwapRequestStatus.APPROVED, userId, "APPROVED");
        notificationService.notifySwapRequestApproved(swapRequest);

        Map<String, Object> data = new HashMap<>();
        data.put("swapRequestId", swapRequest.getId());
        data.put("status", "RESERVED");
        return ApiResponse.success("换位请求已同意", data);
    }

    @Transactional
    public ApiResponse<Void> reject(Long userId, Long swapRequestId) {
        SwapRequest swapRequest = getPendingSwapRequestForUpdate(swapRequestId);
        if (!swapRequest.getTargetUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to reject this swap request");
        }
        markSwapRequestFinished(swapRequest, SwapRequestStatus.REJECTED, userId, "REJECTED");
        notificationService.notifySwapRequestRejected(swapRequest);
        return ApiResponse.success("换位请求已拒绝", null);
    }

    public ApiResponse<Map<String, Object>> check(Long userId, Long reservationId) {
        Reservation targetReservation = reservationRepository.findById(reservationId)
                .orElse(null);

        Map<String, Object> data = new HashMap<>();
        if (targetReservation == null) {
            data.put("canRequest", false);
            data.put("reason", "RESERVATION_NOT_FOUND");
            return ApiResponse.success("OK", data);
        }

        if (!ReservationStatus.isReserved(targetReservation.getStatus())) {
            data.put("canRequest", false);
            data.put("reason", "TARGET_NOT_AVAILABLE");
            return ApiResponse.success("OK", data);
        }

        if (targetReservation.getUserId().equals(userId)) {
            data.put("canRequest", false);
            data.put("reason", "OWN_SEAT");
            return ApiResponse.success("OK", data);
        }

        Optional<Reservation> requesterReservation = findRequesterReservationOptional(userId, targetReservation);
        if (requesterReservation.isEmpty()) {
            data.put("canRequest", false);
            data.put("reason", "TARGET_NOT_AVAILABLE");
            return ApiResponse.success("OK", data);
        }

        if (!swapRequestRepository.findByRequesterUserIdAndTargetReservationIdAndStatus(
                userId, reservationId, SwapRequestStatus.REJECTED).isEmpty()) {
            data.put("canRequest", false);
            data.put("reason", "ALREADY_REJECTED");
            return ApiResponse.success("OK", data);
        }

        if (!swapRequestRepository.findRequesterActiveForSlot(
                userId,
                targetReservation.getReserveDate(),
                targetReservation.getStartTime(),
                targetReservation.getEndTime(),
                SwapRequestStatus.ACTIVE_STATUSES).isEmpty()) {
            data.put("canRequest", false);
            data.put("reason", "HAS_ACTIVE_REQUEST");
            return ApiResponse.success("OK", data);
        }

        if (swapRequestRepository.findFirstByTargetReservationIdAndStatusIn(
                targetReservation.getId(), SwapRequestStatus.ACTIVE_STATUSES).isPresent()) {
            data.put("canRequest", false);
            data.put("reason", "SEAT_LOCKED");
            return ApiResponse.success("OK", data);
        }

        data.put("canRequest", true);
        data.put("reason", "CAN_REQUEST");
        return ApiResponse.success("OK", data);
    }

    public ApiResponse<Map<String, Object>> getMyPending(Long userId, Long reservationId) {
        Optional<SwapRequest> swapRequestOptional = swapRequestRepository.findFirstByTargetReservationIdAndStatusIn(
                reservationId, SwapRequestStatus.ACTIVE_STATUSES);
        if (swapRequestOptional.isEmpty()) {
            return ApiResponse.success("OK", null);
        }

        SwapRequest swapRequest = swapRequestOptional.get();
        if (!swapRequest.getTargetUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "You do not have permission to view this swap request");
        }

        User requester = userRepository.findById(swapRequest.getRequesterUserId())
                .orElse(null);

        Map<String, Object> data = new HashMap<>();
        data.put("swapRequestId", swapRequest.getId());
        data.put("requesterId", swapRequest.getRequesterUserId());
        data.put("requesterName", requester != null ? requester.getUsername() : "User " + swapRequest.getRequesterUserId());
        data.put("message", swapRequest.getMessage());
        data.put("createdAt", swapRequest.getCreatedAt());
        return ApiResponse.success("OK", data);
    }

    public boolean hasActiveSwapRequest(Long reservationId) {
        return !swapRequestRepository.findActiveByReservationId(reservationId, SwapRequestStatus.ACTIVE_STATUSES).isEmpty();
    }

    public Optional<SwapRequest> findActiveByRequesterReservationId(Long reservationId) {
        List<SwapRequest> requests = swapRequestRepository.findByRequesterReservationIdAndStatusIn(
                reservationId, SwapRequestStatus.ACTIVE_STATUSES);
        return requests.stream().findFirst();
    }

    public Optional<SwapRequest> findActiveByTargetReservationId(Long reservationId) {
        return swapRequestRepository.findFirstByTargetReservationIdAndStatusIn(reservationId, SwapRequestStatus.ACTIVE_STATUSES);
    }

    @Scheduled(fixedDelayString = "${swap-request.expire-scan-delay-ms:60000}")
    public void expirePendingRequests() {
        List<Long> ids = swapRequestRepository.findExpirableRequests(SwapRequestStatus.PENDING, LocalDateTime.now())
                .stream().map(SwapRequest::getId).toList();
        for (Long id : ids) {
            try {
                expireSingleRequest(id);
            } catch (Exception ex) {
                log.warn("Failed to expire swap request {}", id, ex);
            }
        }
    }

    @Transactional
    public void expireSingleRequest(Long swapRequestId) {
        SwapRequest swapRequest = swapRequestRepository.findByIdForUpdate(swapRequestId).orElse(null);
        if (swapRequest == null || !SwapRequestStatus.ACTIVE_STATUSES.contains(swapRequest.getStatus())) {
            return;
        }
        if (swapRequest.getExpireAt() != null && swapRequest.getExpireAt().isAfter(LocalDateTime.now())) {
            return;
        }
        markSwapRequestFinished(swapRequest, SwapRequestStatus.EXPIRED, swapRequest.getTargetUserId(), "EXPIRED");
        notificationService.notifySwapRequestExpired(swapRequest);
    }

    private SwapRequest getPendingSwapRequestForUpdate(Long swapRequestId) {
        SwapRequest swapRequest = swapRequestRepository.findByIdForUpdate(swapRequestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Swap request not found"));
        if (!SwapRequestStatus.ACTIVE_STATUSES.contains(swapRequest.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Swap request is no longer pending");
        }
        return swapRequest;
    }

    private Reservation getLockedActiveReservation(Long reservationId) {
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Reservation not found"));
        validateTargetReservation(reservation);
        return reservation;
    }

    private void validateTargetReservation(Reservation reservation) {
        if (!ReservationStatus.isReserved(reservation.getStatus())) {
            throw new ResponseStatusException(BAD_REQUEST, "Target reservation is not available");
        }
    }

    private Reservation findRequesterReservation(Long userId, Reservation targetReservation) {
        return findRequesterReservationOptional(userId, targetReservation)
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "You do not have a matching active reservation for this time slot"));
    }

    private Optional<Reservation> findRequesterReservationOptional(Long userId, Reservation targetReservation) {
        List<Reservation> reservations = reservationRepository.findByUserIdAndReserveDateAndStartTimeAndEndTimeAndStatus(
                userId,
                targetReservation.getReserveDate(),
                targetReservation.getStartTime(),
                targetReservation.getEndTime(),
                ReservationStatus.RESERVED);
        return reservations.stream().filter(r -> !r.getId().equals(targetReservation.getId())).findFirst();
    }

    private void validateAlignedReservations(Reservation requesterReservation, Reservation targetReservation) {
        boolean aligned = requesterReservation.getReserveDate().equals(targetReservation.getReserveDate())
                && requesterReservation.getStartTime().equals(targetReservation.getStartTime())
                && requesterReservation.getEndTime().equals(targetReservation.getEndTime());
        if (!aligned) {
            throw new ResponseStatusException(BAD_REQUEST, "Reservations are no longer aligned for swap");
        }
    }

    private LocalDateTime resolveExpireAt(Reservation targetReservation) {
        LocalDateTime byCreation = LocalDateTime.now().plusHours(24);
        LocalDateTime byStart = LocalDateTime.of(targetReservation.getReserveDate(), targetReservation.getStartTime());
        return byCreation.isBefore(byStart) ? byCreation : byStart;
    }

    private String normalizeMessage(String message) {
        if (message == null) {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private void markSwapRequestFinished(SwapRequest swapRequest, Integer status, Long processedBy, String reason) {
        swapRequest.setStatus(status);
        swapRequest.setProcessedBy(processedBy);
        swapRequest.setProcessedAt(LocalDateTime.now());
        swapRequest.setProcessReason(reason);
        swapRequestRepository.save(swapRequest);
    }
}
