package edu.njit.jerse.ashe.llm.api;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Interface for API client implementations.
 */
public interface ApiClient {

    /**
     * Fetches response from an API based on the provided input.
     *
     * @param input the input to be processed by the API model
     * @param model the model to be used for processing the input
     * @return a String representing the API model's output
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    String fetchApiResponse(String input, String model)
            throws IOException, InterruptedException, ExecutionException, TimeoutException;

    /**
     * Creates an {@link HttpRequest} object suitable for an API, configured with the required headers and body.
     *
     * @param apiRequestBody a {@code String} containing the request body for the API
     * @return an {@link HttpRequest} object configured for the API
     */
    HttpRequest createApiRequest(String apiRequestBody);

    /**
     * Sends the constructed HTTP request to an API and retrieves the response.
     *
     * @param request the {@link HttpRequest} object representing the API request
     * @return an {@link HttpResponse<String>} object containing the API's response
     * @throws IOException if an I/O error occurs
     * @throws InterruptedException if the operation is interrupted
     * @throws ExecutionException if the computation threw an exception
     * @throws TimeoutException if the wait timed out
     */
    HttpResponse<String> getApiResponse(HttpRequest request) throws IOException, InterruptedException, ExecutionException, TimeoutException;

    /**
     * Handles the response received from an API, extracting the relevant output from the response body.
     *
     * @param httpResponse the {@link HttpResponse<String>} object containing the API's response
     * @return a String representing the processed output, or an error message in case of non-200 status codes
     * @throws IOException if processing the response body fails
     */
    String handleApiResponse(HttpResponse<String> httpResponse) throws IOException;
}
