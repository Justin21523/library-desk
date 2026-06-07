package com.justin.libradesk.infrastructure.scheduling;

/**
 * A named background task managed by the {@link JobScheduler}. Use
 * {@link #of(String, Runnable)} to wrap a simple action.
 */
public interface Job {

    String name();

    void run();

    static Job of(String name, Runnable action) {
        return new Job() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void run() {
                action.run();
            }
        };
    }
}
