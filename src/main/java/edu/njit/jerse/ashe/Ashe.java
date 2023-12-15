package edu.njit.jerse.ashe;

import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.utils.JavaCodeCorrector;
import edu.njit.jerse.ashe.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
// TODO: Throughout the project, logs must be updated to fix any misleading or duplicate messages.
// TODO: Fix the project, so we do not need to point to resources and libs in the commands.

/**
 * The {@code Ashe} class orchestrates the correction, minimization, and method
 * replacement processes of Java files, leveraging the Specimin tool, Checker
 * Framework, and GPT-aided error correction to refine and enhance Java code.
 * <p>
 * The ASHE execution flow encompasses:
 * <ol>
 *     <li>
 *         Utilizing the Specimin tool to minimize specified methods in a target Java file,
 *         storing the minimized class in a dedicated 'specimin' directory within the ASHE project.
 *     </li>
 *     <li>
 *         Compiling the minimized file using the Checker Framework to validate its correctness
 *         and identify potential errors.
 *     </li>
 *     <li>
 *         In the presence of errors, sending the problematic code and errors to the ChatGPT API
 *         for corrective suggestions.
 *     </li>
 *     <li>
 *         Extracting code from the ChatGPT API response and overwriting the minimized class
 *         with the suggested corrections.
 *     </li>
 *     <li>
 *         Recompiling the corrected, minimized file using the Checker Framework, repeating the
 *         GPT-guided error correction as needed until no further errors are identified.
 *     </li>
 *     <li>
 *         Once error-free, utilizing the corrected, minimized class to overwrite the original
 *         method that was minimized in the target file, preserving the original file with an
 *         optimized, minimized method.
 *     </li>
 * </ol>
 * ASHE thus provides a comprehensive utility to minimize, validate, and correct Java methods,
 * enhancing code quality through an automated, GPT-aided refinement process.
 *
 * <p>
 * Example usage:
 * <pre>
 *     Ashe ashe = new Ashe();
 *     ashe.run(rootPath, targetFilePath, targetMethodName);
 * </pre>
 * </p>
 */
public class Ashe {
    private static final Logger LOGGER = LogManager.getLogger(Ashe.class);

    /**
     * Orchestrates the running of ASHE's functionality by first minimizing
     * the target file with the Specimin tool, then correcting its errors using GPT,
     * and finally replacing the original method in the target file.
     *
     * @param root         the root path where the target file is located
     * @param targetFile   the Java file to be minimized, corrected, and modified.
     *                     Required format: "[path]/[to]/[package]/ClassName.java"
     *                     Example: "com/example/package/MyClass.java"
     * @param targetMethod the target method in the Java file.
     *                     Required format: "package.name.ClassName#methodName(ParamType1, ParamType2, ...)"
     *                     Parameter types must always be provided, though they can be empty if the method has no parameters.
     *                     For example:
     *                     <ul>
     *                         <li>"com.example.package.MyClass#myMethod(ParamType1, ParamType2)".</li>
     *                         <li>"com.example.package.MyClass#myMethod()". If the method has no parameters.</li>
     *                     </ul>
     * @param model        the model to be used for error correction
     * @throws IOException          if an I/O error occurs during file operations
     * @throws ExecutionException   if an exception was thrown during task execution
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException     if a timeout was encountered during task execution
     */
    public void run(String root, String targetFile, String targetMethod, String model)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Running ASHE...");

        JavaCodeCorrector corrector = new JavaCodeCorrector();

        String speciminTempDir = corrector.minimizeTargetFile(root, targetFile, targetMethod);
        final String sourceFilePath = String.valueOf(Paths.get(speciminTempDir, targetFile));

        boolean errorsReplacedInTargetFile = corrector.fixTargetFileErrorsWithModel(sourceFilePath, targetMethod, model);

        if (!errorsReplacedInTargetFile) {
            if (corrector.checkedFileError(sourceFilePath).isEmpty()) {
                LOGGER.info("No errors found in the file, no replacements needed.");
                LOGGER.info("Exiting...");
                return;
            }

            LOGGER.error("Errors were found but not replaced with {} response.", model);
            throw new RuntimeException("Errors were not replaced with " + model + " response.");
        }
        LOGGER.info("Errors replaced with {} response successfully.", model);

        String methodName = JavaCodeParser.extractMethodName(targetMethod);
        final String originalFilePath = String.valueOf(Paths.get(root, targetFile));
        boolean isOriginalMethodReplaced = MethodReplacementService.replaceOriginalTargetMethod(sourceFilePath, originalFilePath, methodName);

        if (!isOriginalMethodReplaced) {
            LOGGER.error("Original method was not replaced.");
            throw new RuntimeException("Original method was not replaced.");
        }
        LOGGER.info("Original method replaced successfully.");
        LOGGER.info("Exiting...");
    }

    /**
     * Entry point of the ASHE application. It expects three command-line arguments,
     * which are used to initiate the run of ASHE functionality. The arguments specify
     * the root path, target Java file, and target method to be processed. These
     * arguments are necessary to utilize the specified minimization with Specimin.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>
     *                     root path of the target Java file
     *                 </li>
     *                 <li>
     *                     name of the target Java file
     *                 </li>
     *                 <li>
     *                     name and parameter types of the target method within the Java file
     *                 </li>
     *                 <li>
     *                     LLM argument - "gpt-4" or "manual"
     *                     - gpt-4 will run the GPT-4 model
     *                     - manual will run the manual response the user provides in predefined_responses.txt
     *                 </li>
     *             </ol>
     * @throws IOException          if an I/O error occurs during file operations
     * @throws ExecutionException   if an exception was thrown during task execution
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException     if a timeout was encountered during task execution
     */
    public static void main(String[] args)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (args.length != 4) {
            LOGGER.error("Invalid number of arguments provided. Expected arguments: root, targetFile, targetMethod, model. Provided arguments: {}.", Arrays.toString(args));
            throw new IllegalArgumentException("Invalid number of arguments provided. Expected arguments: root, targetFile, targetMethod, model. Provided arguments: " + Arrays.toString(args) + ".");
        }

        // Specimin arguments
        String root = args[0];
        String targetFile = args[1];
        String targetMethod = args[2];

        // LLM argument - either gpt-4 or manual (for now)
        String model = args[3];

        // TODO: Add more models here.
        // Example: Arrays.asList("llama", "palm", "grok");
        Set<String> models = new HashSet<>(Arrays.asList("gpt-4", "manual"));
        if (!models.contains(model)) {
            LOGGER.error("Invalid model argument provided: " + model);
            throw new IllegalArgumentException("Invalid model argument provided: " + model);
        }

        Ashe ashe = new Ashe();
        ashe.run(root, targetFile, targetMethod, model);
    }
}