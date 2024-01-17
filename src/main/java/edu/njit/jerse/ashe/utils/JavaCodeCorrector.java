package edu.njit.jerse.ashe.utils;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import edu.njit.jerse.ashe.llm.mock.MockResponseClient;
import edu.njit.jerse.ashe.llm.api.ApiClient;
import edu.njit.jerse.ashe.services.CheckerFrameworkCompiler;
import edu.njit.jerse.ashe.llm.openai.GptApiClient;
import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.services.SpeciminTool;
import edu.njit.jerse.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

// TODO: Fix the JavaDocs on this file for formatting and accuracy.
// TODO: Actually, check the JavaDocs on all files for formatting and accuracy.

/**
 * Responsible for attempting Java code minimization and correction
 * using a combination of the Specimin tool and GPT-based suggestions,
 * integrated with the Checker Framework to validate code
 * integrity after each compilation.
 * <p>
 * This mechanism tries to minimize code and uses LLM techniques to suggest potential corrections.
 * However, due to the nature of AI-driven solutions, there's no guarantee of absolute accuracy.
 * It should be used as a part of the process of optimizing Java code through method minimization and
 * automated error suggestions, but currently human oversight is recommended.
 */
public class JavaCodeCorrector {

    // TODO: Consider this whole class to be a utility class and make the constructor private.
    private static final Logger LOGGER = LogManager.getLogger(JavaCodeCorrector.class);

    Configuration config = Configuration.getInstance();
    // TODO: Consider that these prompts might need to change depending on how other LLMs respond to them.
    private final String PROMPT_START = config.getPropertyValue("llm.prompt.start");
    private final String PROMPT_END = config.getPropertyValue("llm.prompt.end");
    private static final Pattern TARGET_FILE_PATTERN = Pattern.compile("([a-zA-Z_0-9]+/)*[a-zA-Z_0-9]+\\.java");
    private static final Pattern TARGET_METHOD_PATTERN = Pattern.compile("[a-zA-Z_0-9]+(\\.[a-zA-Z_0-9]+)*#[a-zA-Z_0-9]+\\([^\\)]*\\)");


    /**
     * Utilizes GPT API to attempt to fix errors in the target Java file.
     *
     * @param targetFile   the path to the Java file to be corrected
     * @param targetMethod the target method to be corrected
     * @param model        the model to be used to correct the errors
     * @return true if errors were successfully corrected; false otherwise
     * @throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException
     */
    public boolean fixTargetFileErrorsWithModel(String targetFile, String targetMethod, String model)
            throws IOException, IllegalArgumentException,
            InterruptedException, ExecutionException, TimeoutException {

        ApiClient apiClient = switch (model) {
            case "gpt-4" -> new GptApiClient();
            // TODO: Add these LLM APIs to ASHE and uncomment them here.
            // case "llama" -> new LlamaApiClient();
            // case "palm" -> new PalmApiClient();
            // case "grok" -> new GrokApiClient();
            case "mock" -> new MockResponseClient();
            default -> throw new IllegalStateException("Unexpected value: " + model);
        };

        String errorOutput = checkedFileError(targetFile);
        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return false;
        }

        LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

        while (!errorOutput.isEmpty()) {
            String methodName = JavaCodeParser.extractMethodName(targetMethod);
            ClassOrInterfaceDeclaration checkedClass = JavaCodeParser.extractClassByMethodName(targetFile, methodName);

            String modelCorrection = fetchCorrectionFromModel(apiClient, checkedClass, errorOutput, model);
            if (modelCorrection.isEmpty()) {
                return false;
            }

            boolean wasMethodReplaced = MethodReplacementService.replaceMethodInFile(targetFile, checkedClass.getNameAsString(), modelCorrection);
            if (!wasMethodReplaced) {
                LOGGER.error("Failed to write code to file.");
                return false;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            errorOutput = checkedFileError(targetFile);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
            }
        }

        LOGGER.info("No more errors found in the file.");
        return true;
    }

    /**
     * Fetches a code correction suggestion from the LLM API for a given error in a file compiled with Checker Framework.
     *
     * @param apiClient    the client for fetching responses from the provided LLM API
     * @param checkedClass The class or interface declaration containing the method with errors.
     * @param errorOutput  The error description from the Checker Framework that needs a correction.
     * @return the corrected code block as suggested by the LLM API, or an empty {@code String} if not found.
     * @throws IOException          If there's an error during the API call or parsing.
     * @throws ExecutionException   If the computation threw an exception.
     * @throws InterruptedException If the current thread was interrupted while waiting.
     * @throws TimeoutException     If the wait timed out.
     */
    private String fetchCorrectionFromModel(ApiClient apiClient,
                                            ClassOrInterfaceDeclaration checkedClass, String errorOutput, String model)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {

        String prompt = checkedClass +
                System.lineSeparator() +
                PROMPT_START +
                System.lineSeparator() +
                errorOutput +
                System.lineSeparator() +
                PROMPT_END;

        String gptResponse = apiClient.fetchApiResponse(prompt, model);
        String codeBlock = JavaCodeParser.extractJavaCodeBlockFromResponse(gptResponse);

        if (codeBlock.isEmpty()) {
            LOGGER.error("Could not extract code block from {} response.", model);
            return "";
        }

        LOGGER.info("Code block extracted from {} response:" + System.lineSeparator() + codeBlock, model);
        return codeBlock;
    }

    /**
     * Minimizes a specific method in the target Java file using the Specimin tool.
     *
     * @param root         Root directory of the target file.
     * @param targetFile   Path to the target Java file. The format should adhere to certain specifications.
     * @param targetMethod Method within the target file to minimize. The format should adhere to certain specifications.
     * @return Path to the minimized directory.
     * @throws IOException          If there's an error related to file operations during the minimization process.
     * @throws InterruptedException If the minimization process gets interrupted.
     * @throws RuntimeException     If there's a format error with targetFile or targetMethod, or if the Specimin tool fails to run.
     * @throws InvalidPathException If there's an error while trying to get the path to the minimized directory.
     */
    public Path minimizeTargetFile(String root, String targetFile, String targetMethod)
            throws IOException, InterruptedException {
        if (!isValidTargetFileFormat(targetFile)) {
            String errorMessage = "Formatting error: targetFile does not adhere to the required format.";
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        String adjustedTargetMethod = ensureWhitespaceAfterCommas(targetMethod);
        if (!isValidTargetMethodFormat(adjustedTargetMethod)) {
            String errorMessage = "Formatting error: targetMethod does not adhere to the required format.";
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        LOGGER.info("Minimizing source file...");
        String minimizedDirectory = SpeciminTool.runSpeciminTool(root, targetFile, adjustedTargetMethod);

        if (minimizedDirectory.isEmpty()) {
            String errorMessage = "Specimin tool failed to run or did not produce an output directory.";
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        LOGGER.info("Target file minimized successfully.");

        Path minimizedDirectoryPath;
        try {
            minimizedDirectoryPath = Paths.get(minimizedDirectory);
        } catch (InvalidPathException e) {
            String errorMessage = "An error occurred while trying to get the path to the minimized directory.";
            LOGGER.error(errorMessage, e);
            throw new InvalidPathException(errorMessage, e.getReason());
        }

        return minimizedDirectoryPath;
    }

    /**
     * Checks the target Java file for errors using the Checker Framework Compiler.
     *
     * @param targetFile Path to the Java file to check.
     * @return A string detailing detected errors, or an empty string if none were found.
     */
    public String checkedFileError(String targetFile) {
        String errorOutput;

        try {
            errorOutput = CheckerFrameworkCompiler.compileWithCheckerFramework(targetFile);
            return errorOutput;
        } catch (IOException e) {
            LOGGER.error("An IO error occurred while trying to compile the file: " + targetFile, e);
            // Return an empty string to indicate that
            // no errors were found in the checked file
            return "";
        }
    }

    /**
     * Validates the format of the provided target file path.
     * <p>
     * The expected format is: "[path]/[to]/[package]/ClassName.java".
     * For example: "com/example/package/MyClass.java".
     *
     * @param targetFile the string representing the path to the target Java file.
     * @return true if the targetFile adheres to the expected format, false otherwise.
     */
    private static boolean isValidTargetFileFormat(String targetFile) {
        return TARGET_FILE_PATTERN.matcher(targetFile).matches();
    }

    /**
     * Validates the format of the provided target method.
     * <p>
     * The expected format is: "package.name.ClassName#methodName()"
     * Parameter types must always be provided, though they can be empty if the method has no parameters.
     * For example:
     * <ul>
     *     <li>"com.example.package.MyClass#myMethod(ParamType1, ParamType2)".</li>
     *     <li>"com.example.package.MyClass#myMethod()". If the method has no parameters.</li>
     * </ul>
     *
     * @param targetMethod the string representing the name of the target method.
     * @return true if the targetMethod adheres to the expected format, false otherwise.
     */
    private static boolean isValidTargetMethodFormat(String targetMethod) {
        return TARGET_METHOD_PATTERN.matcher(targetMethod).matches();
    }

    /**
     * Ensures that there is a whitespace after each comma in the given string.
     * If whitespace after a comma is already present or if there are no commas,
     * the original string is returned.
     *
     * <p>
     * This is especially important when trying to compile the target file with
     * Specimin, as it expects a specific format
     *
     * @param input The string to be checked and possibly adjusted.
     * @return The adjusted string with whitespace after commas or the original
     * string if no adjustment is needed.
     */
    public static String ensureWhitespaceAfterCommas(String input) {
        if (input.contains(",") && !input.contains(", ")) {
            return input.replaceAll(",(?! )", ", ");
        }
        return input;
    }
}
