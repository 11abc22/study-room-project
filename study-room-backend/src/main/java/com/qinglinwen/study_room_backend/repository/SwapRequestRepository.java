package com.qinglinwen.study_room_backend.repository;

import com.qinglinwen.study_room_backend.entity.SwapRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SwapRequestRepository extends JpaRepository<SwapRequest, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SwapRequest s where s.id = ?1")
    Optional<SwapRequest> findByIdForUpdate(Long id);

    @Query("""
        select s from SwapRequest s
        where s.requesterReservationId = ?1
          and s.status in ?2
    """)
    List<SwapRequest> findByRequesterReservationIdAndStatusIn(Long requesterReservationId, Collection<Integer> statuses);

    @Query("""
        select s from SwapRequest s
        where s.targetReservationId = ?1
          and s.status in ?2
    """)
    List<SwapRequest> findByTargetReservationIdAndStatusIn(Long targetReservationId, Collection<Integer> statuses);

    @Query("""
        select s from SwapRequest s
        where (s.requesterReservationId = ?1 or s.targetReservationId = ?1)
          and s.status in ?2
    """)
    List<SwapRequest> findActiveByReservationId(Long reservationId, Collection<Integer> statuses);

    @Query("""
        select s from SwapRequest s
        where s.requesterUserId = ?1
          and s.reserveDate = ?2
          and s.startTime = ?3
          and s.endTime = ?4
          and s.status in ?5
    """)
    List<SwapRequest> findRequesterActiveForSlot(Long requesterUserId,
                                                 java.time.LocalDate reserveDate,
                                                 java.time.LocalTime startTime,
                                                 java.time.LocalTime endTime,
                                                 Collection<Integer> statuses);

    @Query("""
        select s from SwapRequest s
        where s.targetReservationId = ?1
          and s.status in ?2
    """)
    Optional<SwapRequest> findFirstByTargetReservationIdAndStatusIn(Long targetReservationId, Collection<Integer> statuses);

    @Query("""
        select s from SwapRequest s
        where s.requesterUserId = ?1
          and s.targetReservationId = ?2
          and s.status = ?3
    """)
    List<SwapRequest> findByRequesterUserIdAndTargetReservationIdAndStatus(Long requesterUserId, Long targetReservationId, Integer status);

    @Query("""
        select s from SwapRequest s
        where s.status = ?1
          and s.expireAt <= ?2
    """)
    List<SwapRequest> findExpirableRequests(Integer status, LocalDateTime now);
}
