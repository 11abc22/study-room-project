package com.qinglinwen.study_room_backend.service;

import java.util.Set;

public final class SwapRequestStatus {

    public static final int PENDING = 1;
    public static final int APPROVED = 2;
    public static final int REJECTED = 3;
    public static final int WITHDRAWN = 4;
    public static final int EXPIRED = 5;

    public static final Set<Integer> ACTIVE_STATUSES = Set.of(PENDING);

    private SwapRequestStatus() {
    }
}
