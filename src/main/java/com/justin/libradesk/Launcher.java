package com.justin.libradesk;

/**
 * Packaging entry point. {@link LibraDeskApplication} extends {@code Application},
 * which the JVM refuses to launch directly from the classpath ("JavaFX runtime
 * components are missing"). A separate launcher that does <em>not</em> extend
 * {@code Application} sidesteps that check, so the jpackage/jlink image can run
 * the app with the JavaFX jars on the classpath. Use this as the main class when
 * packaging; {@code mvn javafx:run} still uses {@link LibraDeskApplication}.
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        LibraDeskApplication.main(args);
    }
}
