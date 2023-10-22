package edu.njit.jerse.models;

/**
 * Represents a choice provided by the ChatGPT API as part of its response.
 * <p>
 * In the context of the ChatGPT API, each request can result in multiple potential outputs or 'choices'.
 * This record captures one individual choice with its details.
 * <p>
 * <strong>Note:</strong> It's important that the field names remain in snake_case for API compatibility.
 */
public record GPTChoice(
        /**
         * The position or ranking of this choice among other potential outputs provided by the API.
         */
        int index,

        /**
         * Encapsulates the role and content of this choice.
         */
        GPTMessage message,

        /**
         * The reason provided by ChatGPT for concluding or stopping at this choice.
         * It may indicate reasons such as reaching the maximum token limit or
         * achieving satisfactory completion of the prompt. It's possible that this
         * field could be empty or null if the API doesn't provide a specific reason
         * for some interactions.
         */
        String finish_reason
) {
}
