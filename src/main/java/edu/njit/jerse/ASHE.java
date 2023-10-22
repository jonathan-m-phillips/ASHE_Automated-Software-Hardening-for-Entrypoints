package edu.njit.jerse;

import edu.njit.jerse.services.CheckerFrameworkCompiler;
import edu.njit.jerse.services.MethodReplacementService;
import edu.njit.jerse.utils.Configuration;
import edu.njit.jerse.utils.JavaCodeParser;
import edu.njit.jerse.services.GPTApiClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

// TODO: ASHE.java was restructured in the development branch.
// TODO: changes were made in this CR, but might need to be looked at again later on.
/**
 * ASHE provides a means to interact with GPT to get suggestions for
 * correcting Java code. The {@link #fixJavaCodeUsingGPT(String)} method
 * is the main entry point for ASHE.
 * <p>
 * This class utilizes the Checker Framework to compile Java code and detect
 * errors. If errors are found, it fetches suggestions from GPT to fix
 * those errors, recompiles the code and overwrites the original code if
 * the suggestions result in code that compiles without errors using the
 * Checker Framework.
 */
public class ASHE {
    private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

    /**
     * Initializes the configuration settings for accessing GPT API and
     * setting up prompts.
     */
    Configuration config = Configuration.getInstance();
    private final String PROMPT_START = config.getPropertyValue("gpt.prompt.start");
    private final String PROMPT_END = config.getPropertyValue("gpt.prompt.end");

    // TODO: Look into adding functionality for multiple classes.
    /**
     * Fixes Java code using suggestions from GPT.
     * <p>
     *
     * @param absoluteClassPath the absolute path to the Java class file to be corrected
     * @throws IOException              if there's an issue accessing the file or writing to it
     * @throws FileNotFoundException    if the provided file path does not point to a valid file
     * @throws IllegalArgumentException if the GPT response is unexpected
     * @throws InterruptedException     if the API call is interrupted
     */
    public void fixJavaCodeUsingGPT(String absoluteClassPath)
            throws IOException, FileNotFoundException, IllegalArgumentException,
            InterruptedException, ExecutionException, TimeoutException {
        String errorOutput = CheckerFrameworkCompiler.compile(absoluteClassPath);
        GPTApiClient gptApiClient = new GPTApiClient();

        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return;
        }

        LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

        while (!errorOutput.isEmpty()) {

            // TODO: This issue was addressed in the development branch. Please see changes there.
            // We are getting a class by a specific method name that is passed in from the command line.
            // We might need to investigate the new method to see if we can extract multiple classes.
            // I.E. com.example.TestClass#testMethod(), where testMethod is the method name.
            // extractClassByMethodName(filePath, methodName);
            // We are doing it this way since we want to get the specific class, but we are only passing in
            // Specimin args. The intent for this is to minimize the amount of arguments needed in the command line.
            // TODO: We are not using Specimin arguments in this branch.
            String methodWithError = String.valueOf(JavaCodeParser.extractFirstClassFromFile(absoluteClassPath));

            String prompt = methodWithError +
                    "\n" +
                    PROMPT_START +
                    "\n" +
                    errorOutput +
                    "\n" +
                    PROMPT_END;

            String gptCorrection = gptApiClient.fetchGPTResponse(prompt);
            String codeBlock = JavaCodeParser.extractJavaCodeBlockFromResponse(gptCorrection);

            if (codeBlock.isEmpty()) {
                LOGGER.error("Could not extract code block from GPT response.");
                return;
            }

            LOGGER.info("Code block extracted from GPT response:" + System.lineSeparator() + codeBlock);

            if (!MethodReplacementService.replaceMethod(absoluteClassPath, codeBlock)) {
                LOGGER.error("Failed to write code to file.");
                return;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = CheckerFrameworkCompiler.compile(absoluteClassPath);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
            }
        }

        LOGGER.info("No more errors found in the file.");
        LOGGER.info("Exiting...");
    }
}