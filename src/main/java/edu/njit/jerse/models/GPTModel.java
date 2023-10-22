package edu.njit.jerse.models;

/**
 * Represents the various models provided by the ChatGPT API.
 * <p>
 * This enum encapsulates the parameters associated with each model,
 * allowing for easy selection and configuration when making requests to the ChatGPT API.
 * <p>
 * <strong>Note:</strong> It's crucial to maintain field names in snake_case for compatibility with the API.
 */
public enum GPTModel {

    // Further models can be added as they are released or used
    /*
    GPT_3_5_TURBO(
            "gpt-3.5-turbo",
            1.0,
            1000,
            1.0,
            0,
            0
    ),
    */

    GPT_4(
            "gpt-4",
            0.2,
            1000,
            0.1,
            0,
            0
    );

    /**
     * The identifier for the GPT model
     */
    private final String model;

    /**
     * Controls the randomness of the output.
     * A higher value makes the output more random, while a lower value makes it more deterministic.
     */
    private final double temperature;

    /**
     * The maximum length of the response in terms of tokens.
     * It limits the response length and can be used to manage response verbosity.
     */
    private final int max_tokens;

    /**
     * Filters the potential response pool to this percentage of most likely choices.
     * It helps in ensuring the generated response's quality by considering only the top likely outputs.
     */
    private final double top_p;

    /**
     * Adjusts the likelihood of tokens appearing based on their frequency in the model's training data.
     * A negative value discourages the use of frequent tokens, while a positive value encourages their use.
     */
    private final int frequency_penalty;

    /**
     * Adjusts the likelihood of tokens appearing based on their presence in the input message.
     * A positive value increases the likelihood of tokens appearing in the input to also appear in the output, and vice versa.
     */
    private final int presence_penalty;

    GPTModel(String model, double temperature, int max_tokens, double top_p, int frequency_penalty, int presence_penalty) {
        this.model = model;
        this.temperature = temperature;
        this.max_tokens = max_tokens;
        this.top_p = top_p;
        this.frequency_penalty = frequency_penalty;
        this.presence_penalty = presence_penalty;
    }

    // Accessor methods with their respective descriptions
    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMax_tokens() {
        return max_tokens;
    }

    public double getTop_p() {
        return top_p;
    }

    public int getFrequency_penalty() {
        return frequency_penalty;
    }

    public int getPresence_penalty() {
        return presence_penalty;
    }
}
