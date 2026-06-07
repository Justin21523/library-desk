package com.justin.libradesk.infrastructure.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs registered {@link Job}s on a single daemon thread and records the outcome
 * of each (last run time, success, detail). Generalizes the former hand-wired
 * maintenance sweep: each maintenance task is now a named job that can also be
 * triggered on demand ({@link #runNow}). A failing job never stops the scheduler.
 */
public class JobScheduler implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JobScheduler.class);

    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "libradesk-jobs");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, Job> jobs = new LinkedHashMap<>();
    private final Map<String, Long> intervals = new LinkedHashMap<>();
    private final Map<String, JobRun> lastRuns = new LinkedHashMap<>();

    /** Registers a job to run every {@code intervalMinutes} (minimum 1). */
    public synchronized void register(Job job, long intervalMinutes) {
        jobs.put(job.name(), job);
        intervals.put(job.name(), Math.max(1, intervalMinutes));
        lastRuns.put(job.name(), new JobRun(job.name(), null, true, "not run yet"));
    }

    /** Schedules every registered job at its interval (first run immediately). */
    public synchronized void start() {
        for (Map.Entry<String, Long> entry : intervals.entrySet()) {
            Job job = jobs.get(entry.getKey());
            executor.scheduleAtFixedRate(() -> runSafely(job), 0, entry.getValue(), TimeUnit.MINUTES);
        }
        log.info("Job scheduler started with {} job(s)", jobs.size());
    }

    /** Runs a job immediately and returns its outcome. */
    public JobRun runNow(String name) {
        Job job = jobs.get(name);
        if (job == null) {
            throw new IllegalArgumentException("Unknown job: " + name);
        }
        return runSafely(job);
    }

    /** @return the last run of each registered job, in registration order. */
    public synchronized List<JobRun> list() {
        return new ArrayList<>(lastRuns.values());
    }

    private JobRun runSafely(Job job) {
        JobRun result;
        try {
            job.run();
            result = new JobRun(job.name(), LocalDateTime.now(), true, "ok");
        } catch (RuntimeException e) {
            log.error("Job {} failed", job.name(), e);
            result = new JobRun(job.name(), LocalDateTime.now(), false, String.valueOf(e.getMessage()));
        }
        synchronized (this) {
            lastRuns.put(job.name(), result);
        }
        return result;
    }

    @Override
    public void close() {
        executor.shutdownNow();
    }
}
