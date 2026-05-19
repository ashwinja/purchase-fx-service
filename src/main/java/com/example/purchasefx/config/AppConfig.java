package com.example.purchasefx.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/**
 * Loads application configuration from Java property files.
 *
 * <p>The loader follows a production-style precedence order while keeping the project
 * dependency-free:</p>
 *
 * <ol>
 *   <li>{@code application.properties} from the classpath</li>
 *   <li>{@code application-{env}.properties} from the classpath when an environment is selected</li>
 *   <li>JVM system properties such as {@code -Dserver.port=9090}</li>
 * </ol>
 *
 * <p>The environment is selected with {@code -Dapp.env=dev|test|prod}. For convenience in
 * deployment tooling, {@code APP_ENV} is also accepted when the JVM property is absent.
 * This is not a {@code .env} file; it is a standard Java properties-file approach.</p>
 */
public final class AppConfig {
    public static final String DEFAULT_ENVIRONMENT = "prod";

    private static final String BASE_PROPERTIES_FILE = "application.properties";

    private final Properties properties;
    private final String environment;

    private AppConfig(Properties properties, String environment) {
        this.properties = properties;
        this.environment = environment;
    }

    /** Loads default configuration and the selected environment-specific property file. */
    public static AppConfig load() {
        String environment = selectedEnvironment();
        Properties loaded = new Properties();
        loadRequired(loaded, BASE_PROPERTIES_FILE);
        loadOptional(loaded, "application-" + environment + ".properties");
        loaded.putAll(System.getProperties());
        return new AppConfig(loaded, environment);
    }

    /** Returns the active environment name such as {@code dev}, {@code test}, or {@code prod}. */
    public String environment() {
        return environment;
    }

    /** Returns the TCP port used by the embedded HTTP server. */
    public int serverPort() {
        return intProperty("server.port");
    }

    /** Returns the JSON Lines file path used by the durable purchase repository. */
    public Path purchaseStorageFile() {
        return Path.of(required("purchase.storage.file"));
    }

    /** Returns the live Treasury FiscalData endpoint used for exchange-rate lookups. */
    public String treasuryApiBaseUrl() {
        return required("treasury.api.base-url");
    }

    private int intProperty(String key) {
        String value = required(key);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Configuration property '" + key + "' must be an integer: " + value, e);
        }
    }

    private String required(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required configuration property: " + key);
        }
        return value.trim();
    }

    private static String selectedEnvironment() {
        String value = System.getProperty("app.env");
        if (value == null || value.isBlank()) {
            value = System.getenv("APP_ENV");
        }
        if (value == null || value.isBlank()) {
            value = DEFAULT_ENVIRONMENT;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static void loadRequired(Properties properties, String resourceName) {
        boolean loaded = loadFromClasspath(properties, resourceName);
        if (!loaded) {
            throw new IllegalStateException("Required configuration file not found on classpath: " + resourceName);
        }
    }

    private static void loadOptional(Properties properties, String resourceName) {
        loadFromClasspath(properties, resourceName);
    }

    private static boolean loadFromClasspath(Properties properties, String resourceName) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try (InputStream stream = classLoader.getResourceAsStream(resourceName)) {
            if (stream == null) {
                return false;
            }
            properties.load(stream);
            return true;
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load configuration file: " + resourceName, e);
        }
    }
}
