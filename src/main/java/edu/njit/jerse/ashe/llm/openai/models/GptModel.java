package edu.njit.jerse.ashe.llm.openai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the various models provided by the ChatGPT API.
 * <p>
 * This enum encapsulates the parameters associated with each model,
 * allowing for easy selection and configuration when making requests to the ChatGPT API.
 */
public enum GptModel {

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
    @JsonProperty("max_tokens")
    private final int maxTokens;

    /**
     * Filters the potential response pool to this percentage of most likely choices.
     * It helps in ensuring the generated response's quality by considering only the top likely outputs.
     */
    @JsonProperty("top_p")
    private final double topP;

    /**
     * Adjusts the likelihood of tokens appearing based on their frequency in the model's training data.
     * A negative value discourages the use of frequent tokens, while a positive value encourages their use.
     */
    @JsonProperty("frequency_penalty")
    private final int frequencyPenalty;

    /**
     * Adjusts the likelihood of tokens appearing based on their presence in the input message.
     * A positive value increases the likelihood of tokens appearing in the input to also appear in the output,
     * and vice versa.
     */
    @JsonProperty("presence_penalty")
    private final int presencePenalty;

    GptModel(String model, double temperature, int maxTokens, double topP, int frequencyPenalty, int presencePenalty) {
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.frequencyPenalty = frequencyPenalty;
        this.presencePenalty = presencePenalty;
    }

    // Accessor methods with their respective descriptions
    public String getModel() {
        return model;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public double getTopP() {
        return topP;
    }

    public int getFrequencyPenalty() {
        return frequencyPenalty;
    }

    public int getPresencePenalty() {
        return presencePenalty;
    }
}
