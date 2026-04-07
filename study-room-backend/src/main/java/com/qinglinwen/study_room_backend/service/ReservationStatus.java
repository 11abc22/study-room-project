package com.qinglinwen.study_room_backend.service;

public final class ReservationStatus {

    public static final int RESERVED = 1;
    public static final int CANCELLED = 2;

    private ReservationStatus() {
    }

    public static boolean isReserved(Integer status) {
        return status != null && status == RESERVED;
    }
}
