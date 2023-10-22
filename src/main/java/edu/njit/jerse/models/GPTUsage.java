package edu.njit.jerse.models;

/**
 * Provides details about the token usage for a ChatGPT API request and response.
 * <p>
 * Given the API's billing and computational considerations, tracking token usage is crucial.
 * <p>
 * <strong>Note:</strong> It's important that the field names remain in snake_case for API compatibility.
 */
public record GPTUsage(
        /**
         * The number of tokens used in the input prompt.
         */
        int prompt_tokens,

        /**
         * The number of tokens used in the API's response or completion.
         */
        int completion_tokens,

        /**
         * The total number of tokens used, typically the sum of prompt and completion tokens.
         */
        int total_tokens
) {
}
