package njit.JerSE.services;

import njit.JerSE.utils.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

/**
 * Provides static methods for compiling Java classes using the Checker Framework.
 * <p>
 * The Checker Framework enhances Java's type system to make it more powerful and expressive,
 * allowing for early error detection in programs. This service utilizes the framework to
 * compile Java classes and detect potential type errors.
 */
public class CheckerFrameworkCompiler {

    private CheckerFrameworkCompiler() {}

    private static final Logger LOGGER = LogManager.getLogger(CheckerFrameworkCompiler.class);

    /**
     * Compiles a Java class using the Checker Framework.
     *
     * @param sourceFile the path to the Java source file that needs to be compiled
     * @return a string containing any errors produced during the compilation
     * TODO: what is returned if there are no errors produced? I think the empty string.
     * @throws IOException If there's an error in executing the compilation command or reading its output
     */
    // TODO: this method is going to need to be modified to compile more than one file at a time.
    // Another concern is that the Java compiler generally needs to be invoked from a specific
    // working directory, which is related to the package of the target class: for example, to
    // compile a file containing the public class com.bar.Foo, the compiler must be invoked
    // on a file whose relative path to the working directory is ./com/bar/Foo.java.
    public static String compile(String sourceFile) throws IOException {
        LOGGER.info("Attempting to compile Java class using Checker Framework: {}", sourceFile);

        // Compilation command with Checker Framework
        String[] command = compileCheckedClassCommand(sourceFile);
        LOGGER.debug("Executing compilation command: {}", String.join(" ", command));

        Process compileProcess = Runtime.getRuntime().exec(command);
        String errorOutput = streamToString(compileProcess.getErrorStream());

        String extractedError = extractError(errorOutput);
        if (!extractedError.isEmpty()) {
            LOGGER.info("Compilation successful for class: {}", sourceFile);
        } else {
            LOGGER.warn("Compilation error for class {}: {}", sourceFile, extractedError);
        }
        return extractError(errorOutput);
    }

    /**
     * Captures the content of an input stream into a string.
     *
     * @param stream the input stream to capture
     * @return a string representation of the stream's content
     * @throws IOException If there's an error in reading from the stream
     */
    // TODO: I don't think we need to implement this ourselves; we could use
    // any of the answers of this SO question: https://stackoverflow.com/questions/309424/how-do-i-read-convert-an-inputstream-into-a-string-in-java.
    // I'd prefer the Guava IOUtils, solution, probably: bringing in that
    // dependency won't pollute our dependency tree, since the CF already depends on it
    // (and we depend on the CF).
    private static String streamToString(InputStream stream) throws IOException {
        LOGGER.debug("Converting input stream to string...");
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            stringBuilder.append(line).append(System.lineSeparator());
        }
        LOGGER.debug("Finished converting stream to string.");
        return stringBuilder.toString();
    }

    /**
     * Extracts an error message from the provided error output string.
     * <p>
     * Specifically, this method searches for the pattern "error:" within the error output string,
     * which is the pattern used by the Checker Framework to indicate an error. If found, it
     * extracts the message that follows that pattern. The error message can span multiple lines.
     * If the "error:" pattern is not found, an empty string is returned.
     *
     * @param errorMessage the error string to extract messages from
     * @return the extracted error message, or an empty string if the "error:" pattern isn't found
     */
    private static String extractError(String errorMessage) {
        LOGGER.debug("Attempting to extract error from error message.");

        /**
         The {@code errorIndex} indicates the position in the input string where the "error:" pattern was found.
         * It can be used to determine the starting point of the extracted error message within the input string.
         * If no "error:" pattern is found, {@code errorIndex} will be -1.
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
        // TODO: The build system should manage this dependency. As a quick improvement, we could make
        // this config file point at the local Maven repository. However, it would be best if we
        // can get this information from Gradle directly.
        // Alternatively, we could modify this code so that rather than shelling out to call the CF,
        // it invokes it directly (and then we add the CF as a Gradle dependency).
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
