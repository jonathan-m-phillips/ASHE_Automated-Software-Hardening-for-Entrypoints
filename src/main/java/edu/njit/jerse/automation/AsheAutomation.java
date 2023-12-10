package edu.njit.jerse.automation;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithName;
import edu.njit.jerse.ashe.ASHE;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code AsheAutomation} class applies {@link ASHE}'s minimization and correction mechanisms to
 * Java files in or under a given directory.
 */
public class AsheAutomation {
  private static final Logger LOGGER = LogManager.getLogger(AsheAutomation.class);

  // A standard Maven/Gradle project structure
  public static final String JAVA_SOURCE_DIR = "src/main/java";

  /**
   * Calls the {@link #processSingleJavaFile(Path, String)} method on each Java file in or under the
   * given directory.
   *
   * @param dirPath the path to the directory to iterate over
   * @param rootPath the root path of the project, used for determining the relative path of each
   *     Java file within the project structure
   * @throws IOException if an I/O error occurs while accessing the directory
   */
  public static void processAllJavaFiles(Path dirPath, String rootPath) throws IOException {
    LOGGER.info("Iterating over Java files in {}", dirPath);

    try (Stream<Path> paths = Files.walk(dirPath)) {
      paths
          .filter(Files::isRegularFile)
          // include only files with .java extension
          .filter(
              path -> path.getFileName() != null && path.getFileName().toString().endsWith(".java"))
          .forEach(
              path -> {
                try {
                  processSingleJavaFile(path, rootPath);
                } catch (IOException
                    | ExecutionException
                    | InterruptedException
                    | TimeoutException e) {
                  LOGGER.error("Error processing Java file: {}", path, e);
                  throw new RuntimeException("Error processing Java file: " + path, e);
                }
              });
    }
    LOGGER.info("Completed iterating over Java files in {}", dirPath);
  }

  /**
   * Processes a Java file using {@link ASHE}. For each public method in a public class, {@link
   * ASHE#run(String, String, String)} is invoked to apply minimization and error correction.
   *
   * @param javaFilePath the path to the Java file to be processed
   * @param projectRootPath the root path of the project, must be a prefix of the javaFile's
   *     absolute path
   * @throws IOException if an I/O error occurs when opening or parsing the file
   * @throws ExecutionException if {@link ASHE} encounters an error during execution
   * @throws InterruptedException if {@link ASHE}'s execution is interrupted
   * @throws TimeoutException if {@link ASHE}'s execution takes longer than the allowed time
   */
  private static void processSingleJavaFile(Path javaFilePath, String projectRootPath)
      throws IOException, ExecutionException, InterruptedException, TimeoutException {
    String javaFileAbsolutePath = javaFilePath.toAbsolutePath().toString();

    LOGGER.info("Processing Java file: {}", javaFileAbsolutePath);
    LOGGER.info("Project root path: {}", projectRootPath);

    if (!javaFileAbsolutePath.startsWith(projectRootPath)) {
      throw new IllegalArgumentException(
          String.format(
              "The project root path %s must be a prefix of the Java file's absolute path %s",
              projectRootPath, javaFileAbsolutePath));
    }

    CompilationUnit cu = StaticJavaParser.parse(javaFilePath);

    // targetFile - the Java file ASHE will target for minimization and error correction
    // Example: edu/njit/jerse/automation/AsheAutomation.java
    String targetFile = formatRelativePathForJavaFile(javaFilePath, projectRootPath);

    // Get the package name if it exists, otherwise use an empty string
    String packageName = cu.getPackageDeclaration().map(NodeWithName::getNameAsString).orElse("");

    // Example: "edu.njit.jerse.automation."
    String packagePrefix = packageName.isEmpty() ? "" : packageName + ".";

    for (TypeDeclaration<?> type : cu.getTypes()) {
      if (type.isPublic()) {
        // Example: AsheAutomation
        String className = type.getNameAsString();

        // Example: edu.njit.jerse.automation.AsheAutomation
        String packageAndClassName = packagePrefix + className;

        for (BodyDeclaration<?> member : type.getMembers()) {
          if (member instanceof MethodDeclaration method) {
            if (method.isPublic()) {
              // targetMethod - the method ASHE will target for minimization and error correction
              // Example: edu.njit.jerse.automation.AsheAutomation#main(String[])
              String targetMethod = fullyQualifiedMethodReference(packageAndClassName, method);

              ASHE ashe = new ASHE();
              ashe.run(projectRootPath, targetFile, targetMethod);
            }
          }
        }
      }
    }
    LOGGER.info("Completed processing Java file: {}", javaFileAbsolutePath);
  }

  /**
   * Finds the relative path from the project root to the specified Java file. It formats the
   * relative path to use forward slashes and strips out the leading source directory path.
   *
   * @param javaFilePath the file path for which the relative path is needed Example:
   *     /absolute/path/src/main/java/com/example/foo/Bar.java
   * @param projectRootPath the absolute path to the root of the project. This path must be a prefix
   *     of the javaFile's absolute path. Example: /absolute/path/src/main/java
   * @return the relative path from the project root to the Java file Example of a relative path:
   *     com/example/foo/Bar.java
   */
  private static String formatRelativePathForJavaFile(Path javaFilePath, String projectRootPath) {
    Path projectRoot = Paths.get(projectRootPath);
    Path absoluteFilePath = javaFilePath.toAbsolutePath();

    String relativePath = projectRoot.relativize(absoluteFilePath).toString();
    relativePath = relativePath.replace(File.separatorChar, '/');

    // Expecting a standard Maven/Gradle project structure
    // I.E. src/main/java
    if (relativePath.startsWith(JAVA_SOURCE_DIR)) {
      relativePath = relativePath.substring(JAVA_SOURCE_DIR.length());
    }

    LOGGER.info("Formatted relative Java file path: {}", relativePath);
    return relativePath;
  }

  /**
   * Formats a fully qualified method reference that includes the package name, class name, method
   * name, and parameter types. This reference is designed to uniquely identify the method for
   * processing by {@link ASHE}.
   *
   * @param packageAndClassName the full package path and class name Example: com.example.foo.Bar
   * @param method the method declaration to identify Example: main(String[])
   * @return a string that represents the fully qualified method reference Example of a fully
   *     qualified method reference: com.example.foo.Bar#main(String[])
   */
  private static String fullyQualifiedMethodReference(
      String packageAndClassName, MethodDeclaration method) {
    String methodName = method.getNameAsString();
    String parameters =
        method.getParameters().stream()
            .map(p -> p.getType().asString())
            .collect(Collectors.joining(", "));

    String methodReference = packageAndClassName + "#" + methodName + "(" + parameters + ")";
    LOGGER.info("Fully qualified method reference: {}", methodReference);

    return methodReference;
  }

  /**
   * The entry point of the application automating {@link ASHE}.
   *
   * @param args command-line arguments, expected order:
   *     <ol>
   *       <li>The absolute path to the directory containing Java files to be processed. Example:
   *           /absolute/path/to/project/src/main/java/com/example/foo
   *       <li>The absolute root path of the project, which is used to determine the relative paths
   *           of Java files during processing. This path is used as a reference to calculate the
   *           relative paths for Java files and should be a common ancestor in the directory
   *           hierarchy for all Java files being processed. Example: /absolute/path/to/project
   *     </ol>
   *
   * @throws IOException if an I/O error occurs while accessing the directory
   */
  public static void main(String[] args) throws IOException {
    if (args.length != 2) {
      LOGGER.error(
          "Invalid number of arguments. Expected 2 arguments: <Directory Path> <Project Root"
              + " Path>");
      System.exit(1);
    }

    String directoryPath = args[0];
    String projectRootPath = args[1];

    Path directory = Paths.get(directoryPath);
    if (!Files.exists(directory) || !Files.isDirectory(directory)) {
      LOGGER.error(
          "The specified directory does not exist or is not a directory: {}", directoryPath);
      System.exit(1);
    }

    processAllJavaFiles(directory, projectRootPath);
  }
}
