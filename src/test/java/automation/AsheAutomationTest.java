package automation;

import static org.junit.jupiter.api.Assertions.*;

import edu.njit.jerse.automation.AsheAutomation;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AsheAutomationTest {
  @SuppressWarnings("initialization.field.uninitialized")
  @TempDir
  Path tempDir;

  @Test
  void shouldProcessJavaFilesInDirectory() throws Exception {
    // Creating two Java files in the temporary directory
    Path javaFile1 = createFile(tempDir, "Test1.java");
    Path javaFile2 = createFile(tempDir, "Test2.java");
    Path txtFile1 = createFile(tempDir, "Test1.txt");

    AsheAutomation.processAllJavaFiles(tempDir, tempDir.toString());
  }

  @Test
  void shouldIgnoreNonJavaFiles() throws Exception {
    Path txtFile = createFile(tempDir, "test.txt");
    long lastModifiedTxtFile = Files.getLastModifiedTime(txtFile).toMillis();

    AsheAutomation.processAllJavaFiles(tempDir, tempDir.toString());

    assertEquals(lastModifiedTxtFile, Files.getLastModifiedTime(txtFile).toMillis());
  }

  @Test
  void shouldHandleEmptyDirectory() throws Exception {
    AsheAutomation.processAllJavaFiles(tempDir, tempDir.toString());

    assertTrue(true);
  }

  private Path createFile(Path directory, String fileName) throws IOException {
    Path filePath = directory.resolve(fileName);
    Files.createFile(filePath);
    assertTrue(Files.exists(filePath));
    return filePath;
  }
}
