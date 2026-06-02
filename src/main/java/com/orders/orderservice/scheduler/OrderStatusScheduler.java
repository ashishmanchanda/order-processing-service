package com.orders.orderservice.scheduler;

import com.orders.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusScheduler {

    private final OrderService orderService;

    /**
     * Runs every 5 minutes.
     * Automatically promotes all PENDING orders to PROCESSING.
     *
     * fixedDelay = 5 minutes (300,000 ms) after the previous execution completes.
     * Can be configured via application.properties: scheduler.order.promote.interval
     */
    @Scheduled(fixedDelayString = "${scheduler.order.promote.interval:300000}")
    public void promotePendingOrdersToProcessing() {
        log.info("=== [Scheduler] Running PENDING → PROCESSING job ===");
        try {
            int count = orderService.promoteWaitingOrdersToProcessing();
            log.info("=== [Scheduler] Promoted {} orders to PROCESSING ===", count);
        } catch (Exception ex) {
            log.error("=== [Scheduler] Error during order promotion job ===", ex);
        }
    }
}
