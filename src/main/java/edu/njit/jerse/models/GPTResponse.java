package edu.njit.jerse.models;

/**
 * Represents a response from the ChatGPT API after initiating a chat conversation.
 * This record encapsulates various details returned by the API after processing a request.
 * <p>
 * When a message or a series of messages are sent to ChatGPT for processing,
 * the API responds with a structured output which this record captures.
 */
public record GPTResponse(
        /**
         * The unique identifier for the chat session.
         */
        String id,

        /**
         * The type of the object returned in the response ("chat").
         */
        String object,

        /**
         * The timestamp indicating when the response was created.
         */
        String created,

        /**
         * The model version used by ChatGPT for this specific response.
         */
        String model,

        /**
         * An Array of potential responses or outputs from ChatGPT.
         * Most prompts only receive one choice, but it is possible to receive multiple choices.
         */
        GPTChoice[] choices,

        /**
         * An object detailing the computational resources (tokens)
         * that were used to process the request and generate the response.
         */
        GPTUsage usage
) {
}
