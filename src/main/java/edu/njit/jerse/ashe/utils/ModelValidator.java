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

    // TODO: Since this class is a utility class, it should not be instantiated.
    // TODO: Add a private constructor to prevent instantiation.

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
            LOGGER.error("No default model found. The list of valid models is empty.");
            throw new IllegalStateException("No default model found. The list of valid models is empty.");
        }

        // TODO: getFirst() not get(0)
        LOGGER.info("Returning default model: " + VALID_MODELS.get(0));
        return VALID_MODELS.get(0);
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
            LOGGER.error("Invalid model argument provided: " + model);
            throw new IllegalArgumentException("Invalid model argument provided: " + model);
        }

        LOGGER.info("Model argument validated successfully: " + model);
    }
}
