package edu.njit.jerse.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class to manage application-specific configurations.
 * <p>
 * This class reads properties from a {@code config.properties} file and
 * provides a mechanism to retrieve property values based on given keys.
 * It assumes that the {@code config.properties} file is located at the root
 * of the classpath.
 */
public class Configuration {
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class);
    private static final Configuration instance = new Configuration();
    private final Properties properties;


    /**
     * Initializes the Configuration by loading the properties from the {@code config.properties} file.
     */
    public Configuration() {
        LOGGER.info("Initializing Configuration...");

        properties = new Properties();

        Class<?> cls = getClass();
        ClassLoader classLoader = cls.getClassLoader();

        if (classLoader == null) {
            LOGGER.error("Class loader is null. Configuration may not be loaded properly.");
            return;
        }

        try (InputStream input = classLoader.getResourceAsStream("config.properties")) {
            if (input == null) {
                LOGGER.warn("Unable to find config.properties. Configuration may not be loaded properly.");
                return;
            }
            properties.load(input);
            LOGGER.info("Successfully loaded config.properties.");
        } catch (IOException ex) {
            LOGGER.error("Exception occurred while loading config.properties: ", ex);
            LOGGER.debug("Detailed exception trace: ", ex);
        }
    }

    public static Configuration getInstance() {
        return instance;
    }

    /**
     * Retrieves the property value for a given key.
     *
     * @param key the key of the property to be retrieved
     * @return the value associated with the provided key
     * @throws IllegalArgumentException If the key is {@code null} or does not exist in the properties.
     */
    public String getPropertyValue(String key) {
        if (key == null) {
            LOGGER.error("Attempted to retrieve a property with a null key.");
            throw new IllegalArgumentException("Property key must not be null");
        }

        String props = properties.getProperty(key);
        if (props == null) {
            LOGGER.warn("No property value found for key: {}", key);
            throw new IllegalArgumentException("Property key does not exist");
        }

        LOGGER.debug("Retrieved property for key {}: {}", key, props);
        return props;
    }
}
