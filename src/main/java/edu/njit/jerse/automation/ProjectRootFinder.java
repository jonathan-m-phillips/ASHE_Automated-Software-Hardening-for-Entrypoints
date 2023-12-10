package edu.njit.jerse.automation;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The {@code ProjectRootFinder} class scans a directory and its subdirectories to find Java project
 * roots. It identifies project roots by looking for directories that follow the standard Java
 * source directory structure, src/main/java, and excludes test-related directories.
 */
public class ProjectRootFinder {
  private static final Logger LOGGER = LogManager.getLogger(ProjectRootFinder.class);

  /**
   * Finds Java project roots in the given directory and its subdirectories.
   *
   * @param directory the root directory from which the search begins
   * @return a list of {@link Path} objects, each representing a discovered Java project root
   */
  public static List<Path> findJavaRoots(Path directory) throws IOException {
    LOGGER.debug("Searching for Java roots in directory: {}", directory.toString());

    List<Path> javaRoots = new ArrayList<>();

    Files.walkFileTree(
        directory,
        new SimpleFileVisitor<>() {
          @Override
          public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
            String dirPath = dir.toString().replace("\\", "/");

            boolean isTestDirectory = dirPath.contains("/test/") || dirPath.contains("/tests/");
            if (isTestDirectory) {
              LOGGER.info("Skipping test directory: {}", dirPath);
              // Skip test directories
              return FileVisitResult.SKIP_SUBTREE;
            }

            boolean isJavaRoot = dirPath.endsWith(AsheAutomation.JAVA_SOURCE_DIR);
            if (isJavaRoot) {
              LOGGER.info("Found Java root: {}", dirPath);
              javaRoots.add(dir);
              // Once a Java source root is found, skip its subdirectories
              return FileVisitResult.SKIP_SUBTREE;
            }

            return FileVisitResult.CONTINUE;
          }
        });

    LOGGER.info("Java root search completed in directory: {}", directory);
    return javaRoots;
  }
}
