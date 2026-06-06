package com.justin.libradesk.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads application configuration from {@code application.properties} on the
 * classpath. Any property may be overridden by an environment variable whose
 * name is the property key in UPPER_SNAKE_CASE (e.g. {@code db.password} ->
 * {@code DB_PASSWORD}), which keeps real credentials out of the source tree.
 */
public final class AppConfig {

    private static final String RESOURCE = "/application.properties";

    private final Properties properties;

    private AppConfig(Properties properties) {
        this.properties = properties;
    }

    /**
     * Loads configuration from the bundled properties file.
     *
     * @throws IllegalStateException if the properties file cannot be read
     */
    public static AppConfig load() {
        Properties props = new Properties();
        try (InputStream in = AppConfig.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing configuration resource: " + RESOURCE);
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read configuration: " + RESOURCE, e);
        }
        return new AppConfig(props);
    }

    public String getString(String key) {
        String value = resolve(key);
        if (value == null) {
            throw new IllegalStateException("Missing configuration key: " + key);
        }
        return value;
    }

    public String getString(String key, String defaultValue) {
        String value = resolve(key);
        return value != null ? value : defaultValue;
    }

    public int getInt(String key) {
        return Integer.parseInt(getString(key));
    }

    public int getInt(String key, int defaultValue) {
        String value = resolve(key);
        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    /** Environment variable wins over the properties file when both are set. */
    private String resolve(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            return env;
        }
        return properties.getProperty(key);
    }
}
