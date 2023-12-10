package edu.njit.jerse.ashe.services;

import edu.njit.jerse.ashe.api.ApiService;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Service implementation for interacting with the OpenAI API.
 *
 * <p>This service provides methods for constructing API requests to OpenAI and retrieving API
 * responses using the provided HTTP client.
 */
public class OpenAIService implements ApiService {
  private static final Logger LOGGER = LogManager.getLogger(OpenAIService.class);

  /**
   * Period (in seconds) after which a log message is emitted indicating the system is waiting for
   * an API response. This is used for scheduled logging to give feedback while waiting for a
   * potentially long-running API call.
   */
  private final long gptResponseLoggingPeriod;

  /** Maximum duration (in seconds) to wait for the API response. */
  private final long gptResponseTimeout;

  /**
   * Default constructor initializes the service with default values for logging period (10 seconds)
   * and response timeout (60 seconds).
   */
  public OpenAIService() {
    this.gptResponseLoggingPeriod = 10;
    this.gptResponseTimeout = 60;
  }

  /**
   * Constructor that allows specifying custom values for the logging period and response timeout.
   *
   * @param gptResponseLoggingPeriod period in seconds for logging waiting messages
   * @param gptResponseTimeout maximum duration in seconds to wait for the API response
   */
  public OpenAIService(long gptResponseLoggingPeriod, long gptResponseTimeout) {
    this.gptResponseLoggingPeriod = gptResponseLoggingPeriod;
    this.gptResponseTimeout = gptResponseTimeout;
  }

  /**
   * Constructs an API request to OpenAI with the provided parameters.
   *
   * @param apiKey the API key for authorization
   * @param apiUri the API endpoint URI
   * @param apiRequestBody the body to send with the request
   * @return a constructed HttpRequest ready to be sent to the OpenAI API
   */
  @Override
  public HttpRequest apiRequest(String apiKey, String apiUri, String apiRequestBody) {
    LOGGER.info("Constructing API request to {}", apiUri);
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(apiUri))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(apiRequestBody))
            .build();

    LOGGER.debug("API request constructed successfully.");

    return request;
  }

  /**
   * Sends the provided HttpRequest asynchronously, logs waiting status at a fixed rate, and
   * retrieves the API response, ensuring a response (or timeout) within a specified duration.
   *
   * <p>This method will send an HTTP request using the provided HttpClient and will wait for a
   * response up to 60 seconds. While waiting for the response, it logs an info message every 10
   * seconds. If a response is not received within 60 seconds, an IOException is thrown and a fatal
   * log message is recorded. It ensures a non-null request and client are provided and throws
   * IllegalArgumentException if either is null.
   *
   * @param request the HttpRequest object to be sent to the API. Must be non-null.
   * @param client the HttpClient used to send the request. Must be non-null.
   * @return HttpResponse&lt;String&gt; the response from the API
   * @throws IllegalArgumentException if either request or client is null
   * @throws IOException if an I/O error occurs, or if the response is not received within 60
   *     seconds
   * @throws InterruptedException if the operation is interrupted
   * @throws ExecutionException if the CompletableFuture throws an exception while getting the
   *     response
   * @throws TimeoutException if waiting for the CompletableFuture times out
   */
  @Override
  public HttpResponse<String> apiResponse(HttpRequest request, HttpClient client)
      throws IOException, InterruptedException, ExecutionException, TimeoutException {

    if (request == null || client == null) {
      throw new IllegalArgumentException("Request or Client cannot be null");
    }

    LOGGER.info("Sending API request to " + request.uri());

    CompletableFuture<HttpResponse<String>> futureResponse =
        client.sendAsync(request, HttpResponse.BodyHandlers.ofString());

    // Log "Waiting for the API response..." every {gptResponseLoggingPeriod} seconds while waiting
    // for the response.
    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    executor.scheduleAtFixedRate(
        () -> LOGGER.info("Waiting for API response..."),
        0,
        gptResponseLoggingPeriod,
        TimeUnit.SECONDS);

    try {
      // Get the API response or throw a TimeoutException if it takes longer than
      // {gptResponseTimeout} seconds.
      HttpResponse<String> response = futureResponse.get(gptResponseTimeout, TimeUnit.SECONDS);

      LOGGER.info("API response received with status code " + response.statusCode());
      return response;
    } catch (TimeoutException e) {
      LOGGER.fatal("API response took too long to be received");
      throw new IOException("API response took too long", e);
    } finally {
      executor.shutdown();
    }
  }
}
