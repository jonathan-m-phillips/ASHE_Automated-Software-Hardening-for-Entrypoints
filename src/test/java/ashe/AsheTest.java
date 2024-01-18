package ashe;

import edu.njit.jerse.ashe.Ashe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class AsheTest {

    private static final String ROOT_PATH = "src/test/resources";
    private static final String TARGET_FILE_PATH = "ashe/TestFileToMinimize.java";
    private static final String TARGET_METHOD_NAME = "ashe.TestFileToMinimize#testSocket(int)";
    private static final String CLASS_FILE = "ashe/TestFileToMinimize.class";

    @Test
    void mockTest() throws Exception {
        String model = "mock";

        String absolutePath = getAbsolutePath();
        assertPathAndFileExist(absolutePath);

        String originalContent = readContent();
        Ashe.run(absolutePath, TARGET_FILE_PATH, TARGET_METHOD_NAME, model);
        String modifiedContent = readContent();

        assertNotEquals(originalContent, modifiedContent, "The file content should be modified.");
    }

    @Test
    void dryRunTest() throws Exception {
        String model = "dryrun";

        String absolutePath = getAbsolutePath();
        assertPathAndFileExist(absolutePath);

        String originalContent = readContent();
        Ashe.run(absolutePath, TARGET_FILE_PATH, TARGET_METHOD_NAME, model);
        String modifiedContent = readContent();

        assertEquals(originalContent, modifiedContent, "The file content should not be modified in dry run mode.");
    }

    /** A .class file is generated when running Ashe. This file is deleted after each test. */
    @AfterEach
    void classCleanUp() throws Exception {
        Path classFilePath = Paths.get(getAbsolutePath(), CLASS_FILE);

        if (Files.exists(classFilePath)) {
            Files.delete(classFilePath);
        }
    }

    private void assertPathAndFileExist(String rootPath) {
        Path path = Paths.get(rootPath, AsheTest.TARGET_FILE_PATH);
        System.out.println("Resolved path: " + path.toAbsolutePath());
        assertTrue(Files.exists(path), "File does not exist at path: " + path);
    }

    private String readContent() throws IOException {
        Path path = Paths.get(getAbsolutePath(), TARGET_FILE_PATH);
        return Files.readString(path);
    }

    private static String getProjectRoot() {
        String root = System.getProperty("user.dir");
        return root != null ? root : "";
    }

    private static String getAbsolutePath() {
        return Paths.get(getProjectRoot(), AsheTest.ROOT_PATH).toString();
    }
}