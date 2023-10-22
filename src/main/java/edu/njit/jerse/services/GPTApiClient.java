package edu.njit.jerse.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.njit.jerse.utils.Configuration;
import edu.njit.jerse.api.ApiService;
import edu.njit.jerse.models.GPTMessage;
import edu.njit.jerse.models.GPTModel;
import edu.njit.jerse.models.GPTRequest;
import edu.njit.jerse.models.GPTResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Provides functionality to interact with the GPT API, facilitating the fetching
 * of corrections based on provided prompts. It encapsulates the process of constructing
 * API requests, sending them, and handling the responses.
 */
public class GPTApiClient {

    private static final Logger LOGGER = LogManager.getLogger(OpenAIService.class);
    Configuration config = Configuration.getInstance();

    private final String API_KEY = config.getPropertyValue("llm.api.key");
    private final String API_URI = config.getPropertyValue("llm.api.uri");
    private final String GPT_SYSTEM = config.getPropertyValue("gpt.message.system");
    private final String GPT_USER = config.getPropertyValue("gpt.message.user");
    private final String GPT_SYSTEM_CONTENT = config.getPropertyValue("gpt.message.system.content");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiService openAIService = new OpenAIService();

    /**
     * Fetches the GPT model's correction output based on the provided prompt.
     *
     * @param prompt the input string to be corrected by the GPT model
     * @return a {@code String} representing the GPT model's output
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws ExecutionException   if the computation threw an exception
     * @throws TimeoutException     if the wait timed out
     */
    public String fetchGPTResponse(String prompt)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        LOGGER.debug("Fetching GPT correction with prompt: {}", prompt);

        String apiRequestBody = createApiRequestBody(prompt);
        HttpRequest request = createApiRequest(apiRequestBody);
        HttpResponse<String> apiResponse = getApiResponse(request);

        LOGGER.debug("fetchGPTResponse: {}", apiResponse);

        return handleApiResponse(apiResponse);
    }

    /**
     * Constructs a GPT request object using the provided prompt.
     *
     * @param prompt a {@code String} to be provided to GPT for generating responses
     * @return a {@code GPTRequest} object configured with the necessary parameters for the GPT API call
     */
    private GPTRequest createGptRequestObject(String prompt) {
        LOGGER.debug("Creating GPT request object with prompt: {}", prompt);

        GPTMessage systemMessage = new GPTMessage(GPT_SYSTEM, GPT_SYSTEM_CONTENT);
        GPTMessage userMessage = new GPTMessage(GPT_USER, prompt);
        GPTMessage[] messages = new GPTMessage[]{systemMessage, userMessage};

        return new GPTRequest(GPTModel.GPT_4, messages);
    }

    /**
     * Converts the GPT request object into a JSON string to be used in the API request body.
     *
     * @param prompt the input string for the GPT model
     * @return a {@code String} containing the JSON representation of the GPT request object
     * @throws JsonProcessingException if processing the JSON content failed
     */
    private String createApiRequestBody(String prompt) throws JsonProcessingException {
        GPTRequest gptRequest = createGptRequestObject(prompt);
        LOGGER.debug("GPT request object created successfully.");
        return objectMapper.writeValueAsString(gptRequest);
    }

    /**
     * Creates an HTTP request object suitable for the GPT API, configured with the required headers and body.
     *
     * @param apiRequestBody a {@code String} containing the JSON representation of the GPT request object
     * @return an {@code HttpRequest} object configured for the GPT API
     */
    private HttpRequest createApiRequest(String apiRequestBody) {
        return openAIService.apiRequest(API_KEY, API_URI, apiRequestBody);
    }

    /**
     * Sends the constructed HTTP request to the GPT API and retrieves the response.
     *
     * @param request the {@code HttpRequest} object representing the API request
     * @return an {@code HttpResponse<String>} object containing the API's response
     * @throws IOException          if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws ExecutionException   if the computation threw an exception
     * @throws TimeoutException     if the wait timed out
     */
    private HttpResponse<String> getApiResponse(HttpRequest request)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {

        HttpClient client = HttpClient.newHttpClient();
        return openAIService.apiResponse(request, client);
    }

    /**
     * Handles the response received from the GPT API, extracting the model's output from the response body.
     *
     * @param httpResponse the {@code HttpResponse<String>} object containing the API's response
     * @return a {@code String} representing the model's output, or an error message in case of non-200 status codes
     * @throws IOException if processing the response body fails
     */
    private String handleApiResponse(HttpResponse<String> httpResponse) throws IOException {
        if (httpResponse.statusCode() == 200) {
            GPTResponse gptResponse = objectMapper.readValue(httpResponse.body(), GPTResponse.class);
            LOGGER.info("Successfully retrieved GPT Prompt response.");
            return gptResponse.choices()[gptResponse.choices().length - 1].message().content();
        } else {
            String errorMsg = "Error:" + System.lineSeparator() + httpResponse.statusCode() + " " + httpResponse.body();
            LOGGER.error(errorMsg);
            return errorMsg;
        }
    }
}
