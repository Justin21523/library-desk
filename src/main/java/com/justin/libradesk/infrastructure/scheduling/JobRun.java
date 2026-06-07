package com.justin.libradesk.infrastructure.scheduling;

import java.time.LocalDateTime;

/**
 * The outcome of the most recent run of a {@link Job}. {@code lastRun} is
 * {@code null} before the job has ever run.
 */
public record JobRun(String name, LocalDateTime lastRun, boolean ok, String detail) {
}
