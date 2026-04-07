package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.SwapRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class LoggingNotificationService implements NotificationService {

    @Override
    public void notifySwapRequestCreated(SwapRequest swapRequest) {
        log.info("Swap request created: id={}, requesterUserId={}, targetUserId={}",
                swapRequest.getId(), swapRequest.getRequesterUserId(), swapRequest.getTargetUserId());
    }

    @Override
    public void notifySwapRequestApproved(SwapRequest swapRequest) {
        log.info("Swap request approved: id={}, requesterUserId={}, targetUserId={}",
                swapRequest.getId(), swapRequest.getRequesterUserId(), swapRequest.getTargetUserId());
    }

    @Override
    public void notifySwapRequestRejected(SwapRequest swapRequest) {
        log.info("Swap request rejected: id={}, requesterUserId={}, targetUserId={}",
                swapRequest.getId(), swapRequest.getRequesterUserId(), swapRequest.getTargetUserId());
    }

    @Override
    public void notifySwapRequestExpired(SwapRequest swapRequest) {
        log.info("Swap request expired: id={}, requesterUserId={}, targetUserId={}",
                swapRequest.getId(), swapRequest.getRequesterUserId(), swapRequest.getTargetUserId());
    }
}
