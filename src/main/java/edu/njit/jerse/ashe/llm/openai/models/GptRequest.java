package edu.njit.jerse.ashe.llm.openai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a request made to the ChatGPT API.
 *
 * <p>
 * Instead of using a composite {@link GptModel} object, this design choice of explicitly listing out the model
 * parameters in the record offers several advantages:
 * </p>
 *
 * <ul>
 *   <li><strong>Direct Field Access:</strong> Embedding the model's parameters directly within the record eliminates
 *       the need to traverse an additional layer of object hierarchy, providing a clearer view of the request
 *       structure as expected by the API.</li>
 *   <li><strong>API Compatibility:</strong> The ChatGPT API anticipates these parameters at the top level of the request.
 *       This flattened structure ensures the Java representation aligns closely with the JSON payload structure,
 *       facilitating easier serialization.</li>
 *   <li><strong>Flexibility:</strong> Decoupling from the {@link GptModel} offers greater flexibility to modify
 *       individual parameters without the need to adjust or instantiate new {@link GptModel} objects.</li>
 * </ul>
 *
 * <p>
 * For a more in-depth explanation of the fields
 * <code>model</code>,
 * <code>temperature</code>,
 * <code>maxTokens</code>,
 * <code>topP</code>,
 * <code>frequencyPenalty</code>,
 * and
 * <code>presencePenalty</code>,
 * please consult the {@link GptModel} class.
 * </p>
 */
public record GptRequest(
        String model,
        double temperature,
        @JsonProperty("max_tokens")
        int maxTokens,
        @JsonProperty("top_p")
        double topP,
        @JsonProperty("frequency_penalty")
        int frequencyPenalty,
        @JsonProperty("presence_penalty")
        int presencePenalty,

        /**
         * An array representing the conversation history with the LLM.
         * Each entry signifies a message in the conversation, allowing the API to understand the context and
         * generate contextually apt responses. Entries can represent 'system', 'user', or 'assistant' messages.
         */
        GptMessage[] messages
) {

    /**
     * Creates a new GptRequest with the provided model configuration and messages.
     *
     * @param modelConfig The configuration for the GPT model, encapsulated as a {@link GptModel}.
     * @param messages    The conversation history or context as an array of {@link GptMessage}.
     */
    public GptRequest(GptModel modelConfig, GptMessage[] messages) {
        this(
                modelConfig.getModel(),
                modelConfig.getTemperature(),
                modelConfig.getMaxTokens(),
                modelConfig.getTopP(),
                modelConfig.getFrequencyPenalty(),
                modelConfig.getPresencePenalty(),
                messages
        );
    }
}