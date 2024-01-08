package edu.njit.jerse.ashe.llm.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.njit.jerse.ashe.llm.api.AbstractApiClient;
import edu.njit.jerse.ashe.llm.api.ApiRequestHandler;
import edu.njit.jerse.ashe.llm.openai.models.GptMessage;
import edu.njit.jerse.ashe.llm.openai.models.GptModel;
import edu.njit.jerse.ashe.llm.openai.models.GptRequest;
import edu.njit.jerse.ashe.llm.openai.models.GptResponse;
import edu.njit.jerse.config.Configuration;
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
public class GptApiClient extends AbstractApiClient {

    private static final Logger LOGGER = LogManager.getLogger(GptApiClient.class);
    Configuration config = Configuration.getInstance();

    private final String GPT_API_KEY = config.getPropertyValue("openai.api.key");
    private final String GPT_API_URI = config.getPropertyValue("openai.api.uri");
    private final String GPT_SYSTEM = config.getPropertyValue("openai.message.system");
    private final String GPT_USER = config.getPropertyValue("openai.message.user");
    private final String GPT_SYSTEM_CONTENT = config.getPropertyValue("openai.message.system.content");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApiRequestHandler openAIService = new OpenAiRequestHandler();

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
    @Override
    public String fetchApiResponse(String prompt, String model)
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
    private GptRequest createGptRequestObject(String prompt) {
        LOGGER.debug("Creating GPT request object with prompt: {}", prompt);

        GptMessage systemMessage = new GptMessage(GPT_SYSTEM, GPT_SYSTEM_CONTENT);
        GptMessage userMessage = new GptMessage(GPT_USER, prompt);
        GptMessage[] messages = new GptMessage[]{systemMessage, userMessage};

        LOGGER.debug("GPT request object created successfully.");
        return new GptRequest(GptModel.GPT_4, messages);
    }

    /**
     * Converts the GPT request object into a JSON string to be used in the API request body.
     *
     * @param prompt the input string for the GPT model
     * @return a {@code String} containing the JSON representation of the GPT request object
     * @throws JsonProcessingException if processing the JSON content failed
     */
    private String createApiRequestBody(String prompt) throws JsonProcessingException {
        GptRequest gptRequest = createGptRequestObject(prompt);
        return objectMapper.writeValueAsString(gptRequest);
    }

    /**
     * Creates an HTTP request object suitable for the GPT API, configured with the required headers and body.
     *
     * @param apiRequestBody a {@code String} containing the JSON representation of the GPT request object
     * @return an {@code HttpRequest} object configured for the GPT API
     */
    @Override
    public HttpRequest createApiRequest(String apiRequestBody) {
        return openAIService.apiRequest(GPT_API_KEY, GPT_API_URI, apiRequestBody);
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
    @Override
    public HttpResponse<String> getApiResponse(HttpRequest request)
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
    @Override
    public String handleApiResponse(HttpResponse<String> httpResponse) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();

        if (httpResponse.statusCode() == 200) {
            GptResponse gptResponse = objectMapper.readValue(httpResponse.body(), GptResponse.class);
            LOGGER.info("Successfully retrieved GPT Prompt response.");
            return gptResponse.choices()[gptResponse.choices().length - 1].message().content();
        } else {
            String errorMsg = "Error:" + System.lineSeparator() + httpResponse.statusCode() + " " + httpResponse.body();
            LOGGER.error(errorMsg);
            return errorMsg;
        }
    }
}
