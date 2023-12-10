package automation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import edu.njit.jerse.automation.GitUtils;
import java.nio.file.Path;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitUtilsTest {

  @SuppressWarnings("initialization.field.uninitialized")
  @TempDir
  Path tempDir;

  @SuppressWarnings("initialization.field.uninitialized")
  Path repoDirectory;

  String repoUrl =
      "https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints"; // Use a real or dummy repository URL
  String branch = "main";

  @BeforeEach
  void setUp() {
    repoDirectory = tempDir;
  }

  @AfterEach
  void tearDown() {
    // Cleanup is handled by the @TempDir feature
  }

  @Test
  void testCloneRepository() {
    assertDoesNotThrow(() -> GitUtils.cloneRepository(repoUrl, branch, repoDirectory));
  }

  @Test
  void testFetchRepository() throws GitAPIException {
    GitUtils.cloneRepository(repoUrl, branch, repoDirectory);
    assertDoesNotThrow(() -> GitUtils.fetchRepository(repoDirectory));
  }
}
