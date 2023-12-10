package edu.njit.jerse.ashe.services;

import com.google.common.io.CharStreams;
import edu.njit.jerse.config.Configuration;
import java.io.*;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides static methods for compiling Java classes using the Checker Framework.
 *
 * <p>The Checker Framework enhances Java's type system to make it more powerful and expressive,
 * allowing for early error detection in programs. This service utilizes the framework to compile
 * Java classes and detect potential type errors.
 */
public final class CheckerFrameworkCompiler {
  private static final Logger LOGGER = LogManager.getLogger(CheckerFrameworkCompiler.class);

  /**
   * Private constructor to prevent instantiation.
   *
   * <p>This class is not meant to be instantiated. All methods are static and can be accessed
   * without creating an instance. Making the constructor private ensures that this class cannot be
   * instantiated from outside the class and helps to prevent misuse.
   */
  private CheckerFrameworkCompiler() {
    throw new AssertionError("Cannot instantiate CheckerFrameworkCompiler");
  }

  /**
   * Compiles a Java class using the Checker Framework.
   *
   * @param classPath the path to the Java class that needs to be compiled
   * @return a string containing any errors produced during the compilation. If there are no errors,
   *     an empty string is returned
   * @throws IOException If there's an error in executing the compilation command or reading its
   *     output
   */
  public static String compileWithCheckerFramework(String classPath) throws IOException {
    LOGGER.info("Attempting to compile Java class using Checker Framework: {}", classPath);

    // Compilation command with Checker Framework
    String[] command = compileCheckedClassCommand(classPath);
    LOGGER.debug("Executing compilation command: {}", String.join(" ", command));

    Process compileProcess = Runtime.getRuntime().exec(command);
    String errorOutput =
        CharStreams.toString(
            new InputStreamReader(compileProcess.getErrorStream(), StandardCharsets.UTF_8));

    String extractedError = extractError(errorOutput);
    if (extractedError.isEmpty()) {
      LOGGER.info("Compilation successful for classPath: {}", classPath);
    } else {
      LOGGER.warn(
          "Compilation error for classPath {}: " + System.lineSeparator() + "{}",
          classPath,
          extractedError);
    }
    return extractError(errorOutput);
  }

  /**
   * Extracts an error message from the provided error output string.
   *
   * <p>Specifically, this method searches for the pattern "error:" within the error output string,
   * which is the pattern used by the Checker Framework to indicate an error. If found, it extracts
   * the message that follows that pattern. The error message can span multiple lines. If the
   * "error:" pattern is not found, an empty string is returned.
   *
   * @param errorMessage the error string to extract messages from
   * @return the extracted error message, or an empty string if the "error:" pattern isn't found
   */
  private static String extractError(String errorMessage) {
    LOGGER.debug("Attempting to extract error from error message.");

    /**
     * The {@code errorIndex} indicates the position in the input string where the "error:" pattern
     * was found. It can be used to determine the starting point of the extracted error message
     * within the input string. If no "error:" pattern is found, {@code errorIndex} will be -1.
     */
    int errorIndex = errorMessage.indexOf("error:");
    if (errorIndex != -1) {
      String extractedError = errorMessage.substring(errorIndex).trim();
      LOGGER.debug("Found error: {}", extractedError);
      // Extract the error message starting from the "error:" pattern
      return extractedError;
    }

    LOGGER.debug("No 'error:' pattern found in the errorMessage.");
    return "";
  }

  /**
   * Constructs the compilation command for a Java class using the Checker Framework.
   *
   * @param checkedClassPath the path to the Java class to be compiled
   * @return an array of strings representing the compilation command
   */
  private static String[] compileCheckedClassCommand(String checkedClassPath) {
    LOGGER.info("Constructing compilation command for Java class: {}", checkedClassPath);

    Configuration config = Configuration.getInstance();
    String checkerJar = config.getPropertyValue("checker.jar.file");
    String checkerClasspath = config.getPropertyValue("checker.classpath");
    String checkerCommands = config.getPropertyValue("checker.commands");

    String[] command = {
      "java",
      "-jar",
      checkerJar,
      "-cp",
      checkerClasspath,
      "-processor",
      checkerCommands,
      checkedClassPath
    };

    LOGGER.debug("Constructed compilation command: {}", String.join(" ", command));
    return command;
  }
}
