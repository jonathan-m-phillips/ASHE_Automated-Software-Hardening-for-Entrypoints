package edu.njit.jerse.ashe.llm.openai.models;

/**
 * Represents a message to or from the ChatGPT API.
 * <p>
 * A message encapsulates content either sent to the API as input or received from the API as a response.
 */
public record GptMessage(
        /**
         * The role of the message, typically indicating whether it's a 'system', 'user', or 'assistant' message.
         */
        String role,

        /**
         * The textual content of the message.
         */
        String content
) {
}
