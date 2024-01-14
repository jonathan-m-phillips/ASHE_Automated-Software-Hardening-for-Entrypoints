package edu.njit.jerse.ashe.services;

import edu.njit.jerse.ashe.Ashe;
import edu.njit.jerse.automation.AsheAutomation;
import edu.njit.jerse.automation.RepositoryAutomationEngine;
import edu.njit.jerse.config.Configuration;
import edu.njit.jerse.ashe.utils.JavaCodeCorrector;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * A utility class to manage and run Specimin - a specification minimizer tool.
 * <p>
 * Specimin preserves a target method and all other specifications that are
 * required to compile it.
 * <p>
 * The SpeciminTool class interfaces with Specimin, providing functionalities to:
 * <ul>
 *     <li>Run the Specimin tool with specified parameters.</li>
 *     <li>Modify the package declaration of the minimized Java file.</li>
 *     <li>Delete minimized directories if needed.</li>
 * </ul>
 */
public final class SpeciminTool {
    private static final Logger LOGGER = LogManager.getLogger(SpeciminTool.class);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is a utility class and is not meant to be instantiated.
     * All methods are static and can be accessed without creating an instance.
     * Making the constructor private ensures that this class cannot be instantiated
     * from outside the class and helps to prevent misuse.
     * </p>
     */
    private SpeciminTool() {
        throw new AssertionError("Cannot instantiate SpeciminTool");
    }

    /**
     * Executes and manages the {@code Specimin} tool using the specified paths and targets.
     *
     * @param root         the root directory for the tool
     * @param targetFile   file to be targeted by the tool
     * @param targetMethod method to be targeted by the tool
     * @return the directory path where the minimized file is saved
     * @throws IOException          if there's an error executing the command or writing the minimized file
     * @throws InterruptedException if the process execution is interrupted
     */
    public static String runSpeciminTool(String root, String targetFile, String targetMethod)
            throws IOException, InterruptedException {
        LOGGER.info("Running SpeciminTool...");

        Configuration config = Configuration.getInstance();
        String speciminPath = config.getPropertyValue("specimin.tool.path");
        Path tempDir = createTempDirectory();

        String argsWithOption = formatSpeciminArgs(tempDir.toString(), root, targetFile, targetMethod);

        List<String> commands = prepareCommands(speciminPath, argsWithOption);

        logCommands(commands);

        startSpeciminProcess(commands, speciminPath);

        return tempDir.toString();
    }

    /**
     * Deletes all files and subdirectories in the Specimin temporary directory. This method is specifically
     * important when automating {@link Ashe} with {@link AsheAutomation} and {@link RepositoryAutomationEngine}.
     * If the temporary directory is not deleted, many directories will be created and require a lot of
     * disk space.
     *
     * @param tempDirPath the {@link Path} to the directory that needs to be deleted
     * @throws IOException if an I/O error occurs during file operations
     */
    public static void deleteSpeciminTempDir(Path tempDirPath) throws IOException {
        if (Files.exists(tempDirPath)) {
            try (Stream<Path> paths = Files.walk(tempDirPath)) {
                paths.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(file -> {
                            LOGGER.info("Deleting file: " + file.getAbsolutePath());
                            boolean fileDeleted = file.delete();
                            if (!fileDeleted) {
                                LOGGER.error("Failed to delete file: " + file.getAbsolutePath());
                            }
                        });
            }
            LOGGER.info("Temporary directory deleted successfully");
            return;
        }
        LOGGER.warn("Specified directory does not exist: " + tempDirPath);
    }

    /**
     * Creates a temporary directory for storing output from the {@code Specimin} tool.
     *
     * @return {@link Path} of the created temporary directory
     */
    private static Path createTempDirectory() {
        try {
            Path tempDir = Files.createTempDirectory("speciminTemp");
            // Delete the temporary directory when the JVM exits.
            // This is a fail-safe in case Ashe#run fails to delete the temporary directory.
            tempDir.toFile().deleteOnExit();
            return tempDir;
        } catch (IOException e) {
            LOGGER.error("Failed to create temporary directory", e);
            throw new RuntimeException("Failed to create temporary directory", e);
        }
    }

    /**
     * Formats the arguments for the {@code Specimin} tool.
     *
     * @param outputDirectory directory for {@code Specimin}'s output
     * @param root            the root directory for the tool
     * @param targetFile      file to be targeted by the tool
     * @param targetMethod    method to be targeted by the tool
     * @return formatted {@code String} of arguments
     */
    private static String formatSpeciminArgs(String outputDirectory, String root, String targetFile, String targetMethod) {
        String adjustedTargetMethod = JavaCodeCorrector.ensureWhitespaceAfterCommas(targetMethod);
        return String.format(
                "--args=" +
                        "--outputDirectory \"%s\" " +
                        "--root \"%s\" " +
                        "--targetFile \"%s\" " +
                        "--targetMethod \"%s\"",
                outputDirectory,
                root,
                targetFile,
                adjustedTargetMethod
        );
    }

    /**
     * Prepares the commands to be executed by the {@code Specimin} tool.
     *
     * @param speciminPath   path to the {@code Specimin} tool
     * @param argsWithOption formatted arguments {@code String}
     * @return {@link List} of commands for execution
     */
    private static List<String> prepareCommands(String speciminPath, String argsWithOption) {
        List<String> commands = new ArrayList<>();
        commands.add(speciminPath + "/gradlew");
        commands.add("run");
        commands.add(argsWithOption);
        return commands;
    }

    /**
     * Logs the commands being executed.
     *
     * @param commands {@link List} of commands to be logged
     */
    private static void logCommands(List<String> commands) {
        LOGGER.info("Executing command:");
        for (String command : commands) {
            LOGGER.info(command);
        }
    }

    /**
     * // TODO: Specimin path should change to using a jar once we are ready
     * Starts the {@code Specimin} process with the given commands and path to the {@code Specimin} project.
     *
     * @param commands     {@link List} of commands to be executed
     * @param speciminPath path to the Specimin tool project // TODO: This may be changed to a jar once we are ready
     * @throws IOException          if there's an error executing the command or reading the output
     * @throws InterruptedException if the process execution is interrupted
     */
    private static void startSpeciminProcess(List<String> commands, String speciminPath)
            throws IOException, InterruptedException {
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        builder.directory(new File(speciminPath));

        Process process;
        try {
            process = builder.start();
        } catch (IOException e) {
            throw new IOException("Failed to start the Specimin process", e);
        }

        logProcessOutput(process);
        finalizeProcess(process);
    }

    /**
     * Logs the output from the {@code Specimin} process.
     *
     * @param process the running {@code Specimin} process
     * @throws IOException if there's an error reading the output
     */
    private static void logProcessOutput(Process process) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                LOGGER.info(line);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to read output from Specimin process", e);
            throw new IOException("Failed to read output from Specimin process", e);
        }
    }

    /**
     * Finalizes the Specimin process by closing streams and destroying the process.
     *
     * @param process the running {@code Specimin} process
     * @throws InterruptedException if the process execution is interrupted
     * @throws IOException          if there's an error closing the streams
     */
    private static void finalizeProcess(Process process) throws InterruptedException, IOException {
        try {
            int exitValue = process.waitFor();
            // Skip throwing an exception if running in dryrun mode so that the process can continue iterations.
            if (exitValue != 0 && !Ashe.MODEL.equals("dryrun")) {
                LOGGER.error("Error executing the command. Exit value: {}", exitValue);
                throw new InterruptedException("Error executing the command. Exit value: " + exitValue);
            }
        } finally {
            process.getErrorStream().close();
            process.getOutputStream().close();
            process.destroy();
        }
    }
}