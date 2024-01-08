package edu.njit.jerse.ashe.llm.openai.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Provides details about the token usage for a ChatGPT API request and response.
 * <p>
 * Given the API's billing and computational considerations, tracking token usage is crucial.
 * <p>
 */
public record GptUsage(
        /**
         * The number of tokens used in the input prompt.
         */
        @JsonProperty("prompt_tokens")
        int promptTokens,

        /**
         * The number of tokens used in the API's response or completion.
         */
        @JsonProperty("completion_tokens")
        int completionTokens,

        /**
         * The total number of tokens used, typically the sum of prompt and completion tokens.
         */
        @JsonProperty("total_tokens")
        int totalTokens
) {
}
