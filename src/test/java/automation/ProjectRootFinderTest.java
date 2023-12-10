package automation;

import static org.junit.jupiter.api.Assertions.*;

import edu.njit.jerse.automation.ProjectRootFinder;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProjectRootFinderTest {
  @SuppressWarnings("initialization.field.uninitialized")
  @TempDir
  Path tempDir;

  @Test
  void testFindJavaRoots_WithJavaRoots() throws IOException {
    Path mainJava = tempDir.resolve("src/main/java");
    Files.createDirectories(mainJava);
    assertTrue(Files.exists(mainJava), "Could not create Java source directory structure.");

    List<Path> javaRoots = ProjectRootFinder.findJavaRoots(tempDir);

    assertEquals(1, javaRoots.size(), "Expected to find one Java root.");
    assertEquals(
        mainJava.toAbsolutePath(),
        javaRoots.get(0).toAbsolutePath(),
        "The Java root paths should match.");
  }

  @Test
  void testFindJavaRoots_WithTestDirectories() throws IOException {
    Path testJava = tempDir.resolve("src/test/java");
    Files.createDirectories(testJava);
    assertTrue(Files.exists(testJava), "Could not create test directory structure.");

    List<Path> javaRoots = ProjectRootFinder.findJavaRoots(tempDir);

    assertTrue(javaRoots.isEmpty(), "Test directories should not be considered as Java roots.");
  }

  @Test
  void testFindJavaRoots_NoJavaRoots() throws IOException {
    List<Path> javaRoots = ProjectRootFinder.findJavaRoots(tempDir);

    assertTrue(javaRoots.isEmpty(), "Should not find Java roots in an empty directory.");
  }
}
