package com.example.demo.common;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility for loading frontend configuration properties from the classpath.
 *
 * <p>Values are read from {@code /com/example/demo/client.properties} only
 * once and then cached in memory. This is typically used to configure the
 * backend base URL and other client-side settings.</p>
 *
 * <p>All methods are {@code static}; the class is not meant to be instantiated.</p>
 */

public final class ClientProps {
    private static final String PATH = "/com/example/demo/client.properties";
    private static Properties PROPS;


    private ClientProps() {}

    /**
     * Lazily loads the {@link Properties} file if it has not been loaded yet.
     *
     * @throws Exception if the properties file cannot be found or read
     */

    private static synchronized void loadOnce() throws Exception {
        if (PROPS != null) return;
        PROPS = new Properties();
        try (InputStream in = ClientProps.class.getResourceAsStream(PATH)) {
            if (in == null) throw new IllegalStateException("client.properties not found at " + PATH);
            PROPS.load(in);
        }
    }

    /**
     * Returns the raw value of a property, or {@code null} if it is not present.
     *
     * @param key the property key to look up
     * @return the property value, or {@code null} if not set
     * @throws Exception if the underlying properties cannot be loaded
     */

    public static String get(String key) throws Exception {
        loadOnce();
        return PROPS.getProperty(key);
    }

    /**
     * Returns the value of a property, or a default value if the property
     * is missing or blank.
     *
     * @param key the property key to look up
     * @param def the default value to return when the property is missing or blank
     * @return the resolved property value or the default
     */

    public static String getOr(String key, String def) {
        try { String v = get(key); return (v == null || v.isBlank()) ? def : v; }
        catch (Exception e) { return def; }
    }

    /**
     * Returns the value of a property and fails if it is missing or blank.
     *
     * @param key the property key to look up
     * @return the non-blank property value
     * @throws IllegalStateException if the property is missing or blank
     * @throws Exception             if the underlying properties cannot be loaded
     */

    public static String require(String key) throws Exception {
        String v = get(key);
        if (v == null || v.isBlank()) throw new IllegalStateException("Missing property: " + key);
        return v;
    }
}
