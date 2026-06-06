package com.justin.libradesk.infrastructure.scheduling;

import com.justin.libradesk.domain.service.CirculationService;
import com.justin.libradesk.domain.service.ReservationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodic background maintenance, run on a single daemon thread:
 * <ul>
 *   <li>marks active loans past their due date as OVERDUE;</li>
 *   <li>expires READY reservations not collected in time and promotes the next.</li>
 * </ul>
 * Both tasks only touch the database, never the UI.
 */
public class MaintenanceScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceScheduler.class);
    private static final String ACTOR = "system";

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "libradesk-maintenance");
        thread.setDaemon(true);
        return thread;
    });

    private final CirculationService circulationService;
    private final ReservationService reservationService;
    private final long periodMinutes;

    public MaintenanceScheduler(CirculationService circulationService,
                                ReservationService reservationService,
                                long periodMinutes) {
        this.circulationService = circulationService;
        this.reservationService = reservationService;
        this.periodMinutes = Math.max(1, periodMinutes);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::runSafely, 0, periodMinutes, TimeUnit.MINUTES);
        log.info("Maintenance sweep scheduled every {} minute(s)", periodMinutes);
    }

    private void runSafely() {
        try {
            circulationService.markOverdueLoans();
            reservationService.expireStaleReady(ACTOR);
        } catch (RuntimeException e) {
            // Never let a failure kill the scheduler; it retries next period.
            log.error("Maintenance sweep failed", e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
