package com.justin.libradesk.infrastructure.scheduling;

import com.justin.libradesk.domain.service.CirculationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Periodically flips active loans that are past their due date to OVERDUE by
 * calling {@link CirculationService#markOverdueLoans()}. Runs on a single daemon
 * thread; the sweep only touches the database, never the UI.
 */
public class OverdueScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(OverdueScheduler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "overdue-sweep");
        thread.setDaemon(true);
        return thread;
    });

    private final CirculationService circulationService;
    private final long periodMinutes;

    public OverdueScheduler(CirculationService circulationService, long periodMinutes) {
        this.circulationService = circulationService;
        this.periodMinutes = Math.max(1, periodMinutes);
    }

    public void start() {
        executor.scheduleAtFixedRate(this::runSafely, 0, periodMinutes, TimeUnit.MINUTES);
        log.info("Overdue sweep scheduled every {} minute(s)", periodMinutes);
    }

    private void runSafely() {
        try {
            circulationService.markOverdueLoans();
        } catch (RuntimeException e) {
            // Never let a failure kill the scheduler; it will retry next period.
            log.error("Overdue sweep failed", e);
        }
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
