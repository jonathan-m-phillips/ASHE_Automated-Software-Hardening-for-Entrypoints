package edu.njit.jerse.ashe.utils;

import edu.njit.jerse.ashe.Ashe;
import edu.njit.jerse.ashe.llm.openai.models.GptModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * This class provides utility methods for validating large language model names. It is designed to
 * ensure that only pre-approved large language model names are used in the application. The list of
 * valid models is maintained internally and can be updated as needed.
 */
public class ModelValidator {
    private static final Logger LOGGER = LogManager.getLogger(ModelValidator.class);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is a utility class and is not meant to be instantiated.
     * All methods are static and can be accessed without creating an instance.
     * Making the constructor private ensures that this class cannot be instantiated
     * from outside the class and helps to prevent misuse.
     * </p>
     */
    private ModelValidator() {
        throw new AssertionError("Cannot instantiate ModelValidator");
    }

    /**
     * A list of valid large language model names supported by the application.
     * <p>
     * The first model in this list is considered the default model for the application.
     * This list can be updated as needed to include new models or remove existing ones.
     * </p>
     *
     * <p>Example models include:</p>
     * <ul>
     *     <li>"gpt-4" - represents the {@link GptModel#GPT_4} model</li>
     *     <li>"mock" - represents a mock model for testing or development purposes</li>
     *     <li>"dryrun" - dryrun allows {@link Ashe#run} to run without a model, skipping the error correction process</li>
     * </ul>
     * TODO: Add more models to this list once they are handled by the application.
     */
    private static final List<String> VALID_MODELS = List.of("gpt-4", "mock", "dryrun");

    /**
     * Gets the default model name. This method returns the first model in the list of valid models.
     *
     * @return a {@code String} with the default model name
     * @throws IllegalStateException if the list of valid models is empty
     */
    public static String getDefaultModel() {
        if (VALID_MODELS.isEmpty()) {
            String errorMessage = "No default model found. The list of valid models is empty.";
            LOGGER.error(errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        LOGGER.info("Returning default model: " + VALID_MODELS.getFirst());
        return VALID_MODELS.getFirst();
    }

    /**
     * Validates the provided model name against a pre-defined set of valid models.
     * If the model name is not in the list of valid models, an {@link IllegalArgumentException} is thrown.
     *
     * @param model the model name to validate
     * @throws IllegalArgumentException if the provided model name is not in the list of valid models
     */
    public static void validateModel(String model) throws IllegalArgumentException {
        if (!VALID_MODELS.contains(model)) {
            String errorMessage = "Invalid model argument provided: " + model;
            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        LOGGER.info("Model argument validated successfully: " + model);
    }
}
