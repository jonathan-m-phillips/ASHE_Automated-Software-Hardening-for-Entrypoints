package edu.njit.jerse.ashe;

import edu.njit.jerse.ashe.llm.openai.models.GptModel;
import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.utils.JavaCodeCorrector;
import edu.njit.jerse.ashe.utils.JavaCodeParser;
import edu.njit.jerse.ashe.utils.ModelValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.plumelib.util.FilesPlume;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
// TODO: Throughout the project, logs must be updated to fix any misleading or duplicate messages.
// TODO: JavaDocs need to be updated throughout the project.
// TODO: Logs and Exceptions sharing the same error message could be stored in a String.

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
 *     Ashe.run(rootPath, targetFilePath, targetMethodName, model);
 * </pre>
 * </p>
 */
public class Ashe {
    private static final Logger LOGGER = LogManager.getLogger(Ashe.class);

    /**
     * The model that will be used for error correction throughout the ASHE process.
     * The initial state of {@code Ashe.MODEL} is the default model, {@link GptModel#GPT_4}.
     */
    public static String MODEL = ModelValidator.getDefaultModel();

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
    public static void run(String root, String targetFile, String targetMethod, String model)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        LOGGER.info("Running ASHE with the {} model...", model);

        JavaCodeCorrector corrector = new JavaCodeCorrector(model);
        Path speciminTempDir;

        try {
            speciminTempDir = corrector.minimizeTargetFile(root, targetFile, targetMethod);
        } catch (InterruptedException e) {
            LOGGER.error("Failed to minimize the target file.");
            LOGGER.info("Skipping...");
            return;
        }

        try {
            String sourceFilePath = "";
            if (speciminTempDir != null) {
                sourceFilePath = speciminTempDir.resolve(targetFile).toString();
            }

            if (model.equals(ModelValidator.DRY_RUN)) {
                LOGGER.info("Dryrun mode enabled. Skipping error correction.");
                return;
            }

            LOGGER.info("Errors replaced with {} response successfully.", model);
            boolean errorsReplacedInTargetFile = corrector.fixTargetFileErrorsWithModel(sourceFilePath, targetMethod, model);

            if (!errorsReplacedInTargetFile) {
                if (corrector.checkedFileError(sourceFilePath).isEmpty()) {
                    LOGGER.info("No errors found in the file, no replacements needed.");
                    LOGGER.info("Exiting...");
                    return;
                }

                LOGGER.error("Errors were found but not replaced with " + model + " response.");
                LOGGER.info("Skipping...");
                return;
            }
            LOGGER.info("Errors replaced with {} response successfully.", model);


            String methodName = JavaCodeParser.extractMethodName(targetMethod);
            final String originalFilePath = Paths.get(root, targetFile).toString();
            boolean isOriginalMethodReplaced = MethodReplacementService.replaceOriginalTargetMethod(sourceFilePath, originalFilePath, methodName);

            if (!isOriginalMethodReplaced) {
                String errorMessage = "Original method was not replaced.";
                LOGGER.error(errorMessage);
                throw new RuntimeException(errorMessage);
            }
        } finally {
            LOGGER.info("Cleaning up temporary directory: " + speciminTempDir);
            boolean deletedTempDir = false;
            if (speciminTempDir != null) {
                deletedTempDir = FilesPlume.deleteDir(speciminTempDir.toFile());
            }
            if (!deletedTempDir) {
                LOGGER.error("Failed to delete temporary directory: " + speciminTempDir);
            }

            LOGGER.info("Exiting...");
        }
    }

    /**
     * Entry point of the ASHE application. It expects three or four command-line arguments,
     * with the fourth argument being optional. The first three arguments specify the root path,
     * target Java file, and target method to be processed, which are necessary for utilizing
     * the Specimin minimization functionality. The optional fourth argument determines the
     * model to be used.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>root path of the target Java file</li>
     *                 <li>name of the target Java file</li>
     *                 <li>name and parameter types of the target method within the Java file</li>
     *                 <li>optional LLM argument:
     *                     <ul>
     *                         <li>"gpt-4" to run the {@link GptModel#GPT_4} model</li>
     *                         <li>"mock" to run the mock response defined in predefined_responses.txt</li>
     *                         <li>"dryrun" to run {@code Ashe#run} without a model, skipping the error correction process</li>
     *                         <li>if this argument is omitted, a default model will be used ({@link GptModel#GPT_4})</li>
     *                     </ul>
     *                 </li>
     *             </ol>
     * @throws IOException          if an I/O error occurs during file operations
     * @throws ExecutionException   if an exception was thrown during task execution
     * @throws InterruptedException if the current thread was interrupted while waiting
     * @throws TimeoutException     if a timeout was encountered during task execution
     */
    public static void main(String[] args)
            throws IOException, ExecutionException, InterruptedException, TimeoutException {
        if (args.length < 3 || args.length > 4) {
            String errorMessage = String.format(
                    "Invalid number of arguments: expected 3 or 4, but received %d. " +
                            "Required arguments: 1) 'root', 2) 'targetFile', 3) 'targetMethod'. " +
                            "Optional: 4) Model name. Provided arguments: %s",
                    args.length, Arrays.toString(args));

            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        // Specimin arguments
        String root = args[0];
        String targetFile = args[1];
        String targetMethod = args[2];

        // If no model is provided, use the default model.
        if (args.length == 3) {
            run(root, targetFile, targetMethod, Ashe.MODEL);
            return;
        }

        Ashe.MODEL = args[3];
        ModelValidator.validateModel(Ashe.MODEL);
        run(root, targetFile, targetMethod, Ashe.MODEL);
    }
}