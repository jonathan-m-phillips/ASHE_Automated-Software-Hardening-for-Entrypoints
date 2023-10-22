package edu.njit.jerse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

// TODO: These changes were requested after the development branch was created, committed, and sent for CR/PR.
// TODO: Therefore, there will be merge conflicts with the development branch that will need to be resolved.
// TODO: Issue in this branch may have been resolved already in development
/**
 * The main entry point for the application.
 * This class handles the initiation of the GPTPrototype to fix Java code.
 */
public class Main {
    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    /**
     * The main method that initializes and runs the GPTPrototype.
     * It expects a single argument which is the class path of the Java class to be checked.
     *
     * @param args command line arguments. The first and only argument should be the class path.
     * @throws IllegalArgumentException if no arguments are provided or if too many arguments are provided
     * @throws IOException              if there's an issue reading or writing files during the code checking and fixing process
     * @throws IllegalStateException    if the API response from the GPT is not as expected
     * @throws InterruptedException     if the API request is interrupted
     */
    public static void main(String[] args) throws IllegalArgumentException, IOException, IllegalStateException, InterruptedException, ExecutionException, TimeoutException {
        if (args.length == 0) {
            LOGGER.error("No arguments provided.");
            throw new IllegalArgumentException("No arguments provided.");
        }

        if (args.length > 1) {
            LOGGER.error("Too many arguments provided.");
            throw new IllegalArgumentException("Too many arguments provided.");
        }

        LOGGER.info("Running ASHE...");

        // class path of the class to be checked
        String classPath = args[0];
        ASHE ashe = new ASHE();
        ashe.fixJavaCodeUsingGPT(classPath);
    }

    // TODO: Specimin is running properly on development branch. Main.java does not exist.
}
