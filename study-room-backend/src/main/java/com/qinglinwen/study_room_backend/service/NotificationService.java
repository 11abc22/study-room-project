package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.SwapRequest;

public interface NotificationService {

    void notifySwapRequestCreated(SwapRequest swapRequest);

    void notifySwapRequestApproved(SwapRequest swapRequest);

    void notifySwapRequestRejected(SwapRequest swapRequest);

    void notifySwapRequestExpired(SwapRequest swapRequest);
}
