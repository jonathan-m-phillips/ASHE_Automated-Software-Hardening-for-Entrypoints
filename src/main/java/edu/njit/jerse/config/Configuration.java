package edu.njit.jerse.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.FileInputStream;
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
    // static field must be set here before initializing in case we get receive
    // an argument in AsheAutomation or RepositoryAutomationEngine for a remote
    // config.properties file
    private static volatile @Nullable Configuration instance;
    private final Properties properties;

    /**
     * Private constructor to prevent instantiation.
     * Initializes the properties from a specified configuration file or from the default classpath location.
     *
     * @param configFilePath optional path to a config.properties file. Nullness is valid, because if null or
     *                       file not found, load config.properties from the local repository. This is essential
     *                       for users who run the application from a remote location and need to specify a
     *                       different classpath.
     */
    private Configuration(@Nullable String configFilePath) {
        LOGGER.info("Initializing Configuration...");
        properties = new Properties();
        if (configFilePath != null && new File(configFilePath).exists()) {
            try (InputStream input = new FileInputStream(configFilePath)) {
                properties.load(input);
                LOGGER.info("Loaded configuration from external file: {}", configFilePath);
            } catch (IOException e) {
                LOGGER.error("Failed to load configuration from {}: {}", configFilePath, e);
            }
            return;
        }

        ClassLoader classLoader = getClass().getClassLoader();
        if (classLoader == null) {
            LOGGER.error("Class loader is null. Configuration may not be loaded properly.");
            return;
        }

        final String CONFIG_PROPERTIES = "config.properties";
        try (InputStream input = classLoader.getResourceAsStream(CONFIG_PROPERTIES)) {
            if (input == null) {
                LOGGER.warn("Unable to find {} in classpath.", CONFIG_PROPERTIES);
                return;
            }

            properties.load(input);
            LOGGER.info("Successfully loaded {} from classpath.", CONFIG_PROPERTIES);

        } catch (IOException e) {
            LOGGER.error("Failed to load {} from classpath: {}", CONFIG_PROPERTIES, e);
        }
    }

    /**
     * Retrieves the singleton instance of Configuration, creating it if necessary.
     * If the instance doesn't exist, it will be created with the specified configuration file path.
     *
     * @param configFilePath the file path to the configuration file. Nullness is valid, because if null or
     *                       file not found, load config.properties from the local repository.
     * @return the singleton instance of the Configuration class
     */
    public static synchronized Configuration getInstance(@Nullable String configFilePath) {
        if (instance == null) {
            instance = new Configuration(configFilePath);
        }
        return instance;
    }

    /**
     * Retrieves the singleton instance of Configuration, creating it if necessary using the default
     * classpath configuration.
     *
     * @return the singleton instance of the {@code Configuration} class
     */
    public static synchronized Configuration getInstance() {
        return getInstance(null);
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
            throw new IllegalArgumentException("Attempted to retrieve a property with a null key.");
        }

        String value = properties.getProperty(key);
        if (value == null) {
            throw new IllegalArgumentException("No property value found for key: " + key);
        }

        return value;
    }
}