package edu.njit.jerse.ashe.llm.mock;

import edu.njit.jerse.ashe.llm.api.AbstractApiClient;
import edu.njit.jerse.config.Configuration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A client for providing predefined responses for API requests.
 * This client does not make actual API calls but rather returns a response from a predefined file.
 * It is useful for scenarios where a mock, static response is needed for testing.
 */
public class MockResponseClient extends AbstractApiClient {

    private final Configuration config = Configuration.getInstance();
    private final String predefinedResponseFile = config.getPropertyValue("mock.response.file");

    /**
     * Fetches a predefined response for an API based on the provided input and model.
     * The actual API call is not made; instead, the response is read from a predefined file.
     *
     * @param prompt ignored in this implementation. Represents the prompt to be processed by an API.
     * @param model  ignored in this implementation. Represents the model to be used for patch synthesis.
     * @return a {@code String} representing the content of the predefined response file
     * @throws IOException if there's an error reading the file
     */
    @Override
    public String fetchApiResponse(String prompt, String model)
            throws IOException {
        return readResponseFromFile(predefinedResponseFile);
    }

    /**
     * Reads and returns the content of a file at the given file path.
     *
     * @param filePath the path to the file to be read
     * @return the content of the file as a {@code String}
     * @throws IOException if an error occurs during file reading
     */
    private String readResponseFromFile(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath));
    }
}