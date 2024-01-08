package edu.njit.jerse.ashe.llm.api;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Abstract base class for API client implementations.
 * This class provides default, often unsupported, implementations for the methods
 * of the {@link ApiClient} interface. Subclasses are expected to override these methods
 * with concrete implementations specific to their respective API interactions.
 */
public abstract class AbstractApiClient implements ApiClient {

    /**
     * Fetches a response from an API based on the provided input and model.
     * This method should be overridden by subclasses to implement specific API fetching logic.
     *
     * @param input the input to be processed by the API model
     * @param model the model to be used for processing the input
     * @return a {@code String} representing the API model's output
     * @throws IOException                   if an I/O error occurs
     * @throws InterruptedException          if the operation is interrupted
     * @throws ExecutionException            if the computation threw an exception
     * @throws TimeoutException              if the wait timed out
     * @throws UnsupportedOperationException if the method is not implemented
     */
    @Override
    public String fetchApiResponse(String input, String model)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("fetchApiResponse not implemented");
    }

    /**
     * Creates an {@link HttpRequest} object suitable for an API, configured with the required headers and body.
     * This method should be overridden by subclasses to construct specific HttpRequest objects.
     *
     * @param apiRequestBody a {@code String} containing the request body for the API
     * @return an {@link HttpRequest} object configured for the API
     * @throws UnsupportedOperationException if the method is not implemented
     */
    @Override
    public HttpRequest createApiRequest(String apiRequestBody) {
        throw new UnsupportedOperationException("createApiRequest not implemented");
    }

    /**
     * Sends the constructed {@link HttpRequest} to an API and retrieves the response.
     * This method should be overridden by subclasses to send HTTP requests and handle responses.
     *
     * @param request the HttpRequest object representing the API request
     * @return an {@link HttpResponse} object containing the API's response
     * @throws IOException                   if an I/O error occurs
     * @throws InterruptedException          if the operation is interrupted
     * @throws ExecutionException            if the computation threw an exception
     * @throws TimeoutException              if the wait timed out
     * @throws UnsupportedOperationException if the method is not implemented
     */
    @Override
    public HttpResponse<String> getApiResponse(HttpRequest request)
            throws IOException, InterruptedException, ExecutionException, TimeoutException {
        throw new UnsupportedOperationException("getApiResponse not implemented");
    }

    /**
     * Handles the response received from an API, extracting the relevant output from the response body.
     * This method should be overridden by subclasses to process API responses.
     *
     * @param httpResponse the HttpResponse object containing the API's response
     * @return a {@code String} representing the processed output, or an error message in case of non-200 status codes
     * @throws IOException                   if processing the response body fails
     * @throws UnsupportedOperationException if the method is not implemented
     */
    @Override
    public String handleApiResponse(HttpResponse<String> httpResponse) throws IOException {
        throw new UnsupportedOperationException("handleApiResponse not implemented");
    }
}