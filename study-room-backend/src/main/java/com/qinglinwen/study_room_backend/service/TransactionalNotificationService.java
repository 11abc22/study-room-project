package com.qinglinwen.study_room_backend.service;

import com.qinglinwen.study_room_backend.entity.SwapRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@Primary
public class TransactionalNotificationService implements NotificationService {

    private final NotificationService delegate;
    private final NotificationDispatchExecutor dispatchExecutor;

    public TransactionalNotificationService(@Qualifier("notificationServiceDelegate") NotificationService delegate,
                                            NotificationDispatchExecutor dispatchExecutor) {
        this.delegate = delegate;
        this.dispatchExecutor = dispatchExecutor;
    }

    @Override
    public void notifySwapRequestCreated(SwapRequest swapRequest) {
        dispatchAfterCommit(() -> delegate.notifySwapRequestCreated(swapRequest));
    }

    @Override
    public void notifySwapRequestApproved(SwapRequest swapRequest) {
        dispatchAfterCommit(() -> delegate.notifySwapRequestApproved(swapRequest));
    }

    @Override
    public void notifySwapRequestRejected(SwapRequest swapRequest) {
        dispatchAfterCommit(() -> delegate.notifySwapRequestRejected(swapRequest));
    }

    @Override
    public void notifySwapRequestExpired(SwapRequest swapRequest) {
        dispatchAfterCommit(() -> delegate.notifySwapRequestExpired(swapRequest));
    }

    private void dispatchAfterCommit(Runnable action) {
        Runnable safeAction = () -> {
            try {
                action.run();
            } catch (Exception ex) {
                log.warn("Failed to dispatch swap notification", ex);
            }
        };

        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchExecutor.execute(safeAction);
                }
            });
            return;
        }

        dispatchExecutor.execute(safeAction);
    }
}
