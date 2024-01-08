package ashe;

import edu.njit.jerse.ashe.Ashe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class AsheTest {

//    private final static String absolutePath = "/your/absolute/path/to/ASHE_Automated-Software-Hardening-for-Entrypoints";
    private final static String absolutePath = "/Users/jonathanphillips/Desktop/NJIT/Research/ASHE/ASHE_Automated-Software-Hardening-for-Entrypoints";
    private final static String rootPath = "/src/test/resources";
    private final static String absoluteRootPath = absolutePath + rootPath;
    private final static String targetFilePath = "ashe/TestFileToMinimize.java";
    private final static String targetMethodName = "ashe.TestFileToMinimize#testSocket(int)";
    private final static String classFile = "ashe/TestFileToMinimize.class";

    @Test
    void mockTest() throws Exception {
        String model = "mock";

        String originalContent = readContent();
        Ashe.run(absoluteRootPath, targetFilePath, targetMethodName, model);
        String modifiedContent = readContent();

        assertNotEquals(originalContent, modifiedContent, "The file content should be modified.");
    }

    @Test
    void dryRunTest() throws Exception {
        String model = "dryrun";

        String originalContent = readContent();
        Ashe.run(absoluteRootPath, targetFilePath, targetMethodName, model);
        String modifiedContent = readContent();

        assertEquals(originalContent, modifiedContent, "The file content should not be modified in dry run mode.");
    }

    /** A .class file is generated when running Ashe. This file is deleted after each test. */
    @AfterEach
    void classCleanUp() throws Exception {
        Path classFilePath = Paths.get(absoluteRootPath, classFile);

        if (Files.exists(classFilePath)) {
            Files.delete(classFilePath);
        }
    }

    private String readContent() throws IOException {
        Path path = Paths.get(absoluteRootPath, targetFilePath);
        return Files.readString(path);
    }
}