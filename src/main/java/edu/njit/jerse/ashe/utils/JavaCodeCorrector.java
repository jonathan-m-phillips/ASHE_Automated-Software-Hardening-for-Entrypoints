package edu.njit.jerse.ashe.utils;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import edu.njit.jerse.ashe.ASHE;
import edu.njit.jerse.ashe.services.CheckerFrameworkCompiler;
import edu.njit.jerse.ashe.services.GPTApiClient;
import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.services.SpeciminTool;
import edu.njit.jerse.config.Configuration;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Responsible for attempting Java code minimization and correction using a combination of the
 * Specimin tool and GPT-based suggestions, integrated with the Checker Framework to validate code
 * integrity after each compilation.
 *
 * <p>This mechanism tries to minimize code and uses LLM techniques to suggest potential
 * corrections. However, due to the nature of AI-driven solutions, there's no guarantee of absolute
 * accuracy. It should be used as a part of the process of optimizing Java code through method
 * minimization and automated error suggestions, but currently human oversight is recommended.
 */
public class JavaCodeCorrector {

  private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

  Configuration config = Configuration.getInstance();
  private final String PROMPT_START = config.getPropertyValue("gpt.prompt.start");
  private final String PROMPT_END = config.getPropertyValue("gpt.prompt.end");
  private static final Pattern TARGET_FILE_PATTERN =
      Pattern.compile("([a-zA-Z_0-9]+/)*[a-zA-Z_0-9]+\\.java");
  private static final Pattern TARGET_METHOD_PATTERN =
      Pattern.compile("[a-zA-Z_0-9]+(\\.[a-zA-Z_0-9]+)*#[a-zA-Z_0-9]+\\([^\\)]*\\)");

  /**
   * Utilizes GPT API to attempt to fix errors in the target Java file.
   *
   * @param targetFile The path to the Java file to be corrected.
   * @param targetMethod The target method to be corrected.
   * @return true if errors were successfully corrected; false otherwise.
   * @throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException,
   *     ExecutionException, TimeoutException
   */
  public boolean fixTargetFileErrorsWithGPT(String targetFile, String targetMethod)
      throws IOException,
          IllegalArgumentException,
          InterruptedException,
          ExecutionException,
          TimeoutException {

    GPTApiClient gptApiClient = new GPTApiClient();

    String errorOutput = checkedFileError(targetFile);
    if (errorOutput.isEmpty()) {
      LOGGER.info("No errors found in the file.");
      return false;
    }

    LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

    while (!errorOutput.isEmpty()) {
      String methodName = JavaCodeParser.extractMethodName(targetMethod);
      ClassOrInterfaceDeclaration checkedClass =
          JavaCodeParser.extractClassByMethodName(targetFile, methodName);

      String gptCorrection = fetchCorrectionFromGPT(gptApiClient, checkedClass, errorOutput);
      if (gptCorrection.isEmpty()) {
        return false;
      }

      boolean wasMethodReplaced =
          MethodReplacementService.replaceMethodInFile(
              targetFile, checkedClass.getNameAsString(), gptCorrection);
      if (!wasMethodReplaced) {
        LOGGER.error("Failed to write code to file.");
        return false;
      }

      LOGGER.info(
          "File written successfully. Recompiling with Checker Framework to check for additional"
              + " warnings...");

      errorOutput = checkedFileError(targetFile);

      if (!errorOutput.isEmpty()) {
        LOGGER.warn(
            "Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
      }
    }

    LOGGER.info("No more errors found in the file.");
    return true;
  }

  /**
   * Fetches a code correction suggestion from the GPT API for a given error in a file compiled with
   * Checker Framework.
   *
   * @param gptApiClient The client for fetching responses from the GPT API.
   * @param checkedClass The class or interface declaration containing the method with errors.
   * @param errorOutput The error description from the Checker Framework that needs a correction.
   * @return The corrected code block as suggested by the GPT API, or an empty string if not found.
   * @throws IOException If there's an error during the API call or parsing.
   * @throws ExecutionException If the computation threw an exception.
   * @throws InterruptedException If the current thread was interrupted while waiting.
   * @throws TimeoutException If the wait timed out.
   */
  private String fetchCorrectionFromGPT(
      GPTApiClient gptApiClient, ClassOrInterfaceDeclaration checkedClass, String errorOutput)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {

    String prompt =
        checkedClass
            + System.lineSeparator()
            + PROMPT_START
            + System.lineSeparator()
            + errorOutput
            + System.lineSeparator()
            + PROMPT_END;

    String gptResponse = gptApiClient.fetchGPTResponse(prompt);
    String codeBlock = JavaCodeParser.extractJavaCodeBlockFromResponse(gptResponse);

    if (codeBlock.isEmpty()) {
      LOGGER.error("Could not extract code block from GPT response.");
    } else {
      LOGGER.info("Code block extracted from GPT response:" + System.lineSeparator() + codeBlock);
    }

    return codeBlock;
  }

  /**
   * Minimizes a specific method in the target Java file using the Specimin tool.
   *
   * @param root Root directory of the target file.
   * @param targetFile Path to the target Java file. The format should adhere to certain
   *     specifications.
   * @param targetMethod Method within the target file to minimize. The format should adhere to
   *     certain specifications.
   * @return The directory where the minimized file is saved.
   * @throws IOException If there's an error related to file operations during the minimization
   *     process.
   * @throws InterruptedException If the minimization process gets interrupted.
   * @throws RuntimeException If there's a format error with targetFile or targetMethod, or if the
   *     Specimin tool fails to run.
   */
  public String minimizeTargetFile(String root, String targetFile, String targetMethod)
      throws IOException, InterruptedException {
    if (!isValidTargetFileFormat(targetFile)) {
      LOGGER.error("Formatting error: targetFile does not adhere to the required format.");
      throw new RuntimeException(
          "Formatting error: targetFile does not adhere to the required format.");
    }

    String adjustedTargetMethod = ensureWhitespaceAfterCommas(targetMethod);
    if (!isValidTargetMethodFormat(adjustedTargetMethod)) {
      LOGGER.error("Formatting error: targetMethod does not adhere to the required format.");
      throw new RuntimeException(
          "Formatting error: targetMethod does not adhere to the required format.");
    }

    LOGGER.info("Minimizing source file...");
    String minimizedDirectory =
        SpeciminTool.runSpeciminTool(root, targetFile, adjustedTargetMethod);

    if (minimizedDirectory.isEmpty()) {
      LOGGER.error("Specimin tool failed to run or did not produce an output directory.");
      throw new RuntimeException(
          "Specimin tool failed to run or did not produce an output directory.");
    }

    LOGGER.info("Target file minimized successfully.");
    return minimizedDirectory;
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
   *
   * <p>The expected format is: "[path]/[to]/[package]/ClassName.java". For example:
   * "com/example/package/MyClass.java".
   *
   * @param targetFile the string representing the path to the target Java file.
   * @return true if the targetFile adheres to the expected format, false otherwise.
   */
  private static boolean isValidTargetFileFormat(String targetFile) {
    return TARGET_FILE_PATTERN.matcher(targetFile).matches();
  }

  /**
   * Validates the format of the provided target method.
   *
   * <p>The expected format is: "package.name.ClassName#methodName()" Parameter types must always be
   * provided, though they can be empty if the method has no parameters. For example:
   *
   * <ul>
   *   <li>"com.example.package.MyClass#myMethod(ParamType1, ParamType2)".
   *   <li>"com.example.package.MyClass#myMethod()". If the method has no parameters.
   * </ul>
   *
   * @param targetMethod the string representing the name of the target method.
   * @return true if the targetMethod adheres to the expected format, false otherwise.
   */
  private static boolean isValidTargetMethodFormat(String targetMethod) {
    return TARGET_METHOD_PATTERN.matcher(targetMethod).matches();
  }

  /**
   * Ensures that there is a whitespace after each comma in the given string. If whitespace after a
   * comma is already present or if there are no commas, the original string is returned.
   *
   * <p>This is especially important when trying to compile the target file with Specimin, as it
   * expects a specific format
   *
   * @param input The string to be checked and possibly adjusted.
   * @return The adjusted string with whitespace after commas or the original string if no
   *     adjustment is needed.
   */
  public static String ensureWhitespaceAfterCommas(String input) {
    if (input.contains(",") && !input.contains(", ")) {
      return input.replaceAll(",(?! )", ", ");
    }
    return input;
  }
}
