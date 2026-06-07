package com.justin.libradesk.infrastructure.scheduling;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JobSchedulerTest {

    @Test
    void runNowExecutesJobAndRecordsSuccess() {
        JobScheduler scheduler = new JobScheduler();
        AtomicInteger runs = new AtomicInteger();
        scheduler.register(Job.of("counter", runs::incrementAndGet), 60);

        JobRun result = scheduler.runNow("counter");

        assertEquals(1, runs.get());
        assertTrue(result.ok());
        assertNotNull(result.lastRun());
        assertEquals(result, scheduler.list().get(0));
    }

    @Test
    void listShowsNotRunYetBeforeFirstRun() {
        JobScheduler scheduler = new JobScheduler();
        scheduler.register(Job.of("idle", () -> { }), 60);

        JobRun run = scheduler.list().get(0);
        assertNull(run.lastRun());
        assertTrue(run.ok());
    }

    @Test
    void failingJobIsRecordedAsNotOk() {
        JobScheduler scheduler = new JobScheduler();
        scheduler.register(Job.of("boom", () -> {
            throw new IllegalStateException("kaboom");
        }), 60);

        JobRun result = scheduler.runNow("boom");

        assertFalse(result.ok());
        assertEquals("kaboom", result.detail());
    }

    @Test
    void runNowRejectsUnknownJob() {
        JobScheduler scheduler = new JobScheduler();
        assertThrows(IllegalArgumentException.class, () -> scheduler.runNow("missing"));
    }
}
