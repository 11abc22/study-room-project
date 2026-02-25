package com.qinglinwen.study_room_backend.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationDispatchExecutor {

    @Async
    public void execute(Runnable task) {
        try {
            task.run();
        } catch (Exception ex) {
            log.warn("Asynchronous notification dispatch failed", ex);
        }
    }
}
