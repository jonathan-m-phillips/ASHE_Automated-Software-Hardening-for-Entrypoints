package edu.njit.jerse.models;

/**
 * Represents a request made to the ChatGPT API.
 *
 * <p>
 * Instead of using a composite {@link GPTModel} object, this design choice of explicitly listing out the model
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
 *   <li><strong>Flexibility:</strong> Decoupling from the {@link GPTModel} offers greater flexibility to modify
 *       individual parameters without the need to adjust or instantiate new {@link GPTModel} objects.</li>
 * </ul>
 *
 * <p>
 * For a more in-depth explanation of the fields
 * <code>model</code>,
 * <code>temperature</code>,
 * <code>max_tokens</code>,
 * <code>top_p</code>,
 * <code>frequency_penalty</code>,
 * and
 * <code>presence_penalty</code>,
 * please consult the {@link GPTModel} class.
 * </p>
 */
public record GPTRequest(
        String model,
        double temperature,
        int max_tokens,
        double top_p,
        int frequency_penalty,
        int presence_penalty,

        /**
         * An array representing the conversation history with the LLM.
         * Each entry signifies a message in the conversation, allowing the API to understand the context and
         * generate contextually apt responses. Entries can represent 'system', 'user', or 'assistant' messages.
         */
        GPTMessage[] messages
) {

    /**
     * Creates a new GPTRequest with the provided model configuration and messages.
     *
     * @param modelConfig The configuration for the GPT model, encapsulated as a {@link GPTModel}.
     * @param messages    The conversation history or context as an array of {@link GPTMessage}.
     */
    public GPTRequest(GPTModel modelConfig, GPTMessage[] messages) {
        this(
                modelConfig.getModel(),
                modelConfig.getTemperature(),
                modelConfig.getMax_tokens(),
                modelConfig.getTop_p(),
                modelConfig.getFrequency_penalty(),
                modelConfig.getPresence_penalty(),
                messages
        );
    }
}