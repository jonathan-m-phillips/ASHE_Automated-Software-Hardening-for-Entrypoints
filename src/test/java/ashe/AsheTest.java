package ashe;

import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.utils.JavaCodeParser;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Paths;

class AsheTest {

    @Test
    void runAsheTest() throws IOException, InterruptedException {
        // TODO: Add your own paths
        final String responseFilePath = "src/test/resources/ashe/predefined_response.txt";
        final String root = "/absolute/path/to/projectRoot/src/main/java";
        final String targetFile = "com/example/SocketClient.java";
        final String targetMethod = "com.example.SocketClient#testSocket(int)";

        JavaCodeCorrectorTest correctorTest = new JavaCodeCorrectorTest(responseFilePath);

        String speciminTempDir = correctorTest.minimizeTargetFile(root, targetFile, targetMethod);
        assertNotNull(speciminTempDir, "Specimin did not return a temp directory");
        final String sourceFilePath = Paths.get(speciminTempDir, targetFile).toString();

        boolean errorsReplacedInTargetFile = correctorTest.fixTargetFileErrorsWithGptSimulation(sourceFilePath, targetMethod);
        assertTrue(errorsReplacedInTargetFile, "Errors were not successfully replaced in the target file");

        String methodName = JavaCodeParser.extractMethodName(targetMethod);
        String originalFilePath = Paths.get(root, targetFile).toString();
        boolean isOriginalMethodReplaced = MethodReplacementService.replaceOriginalTargetMethod(sourceFilePath, originalFilePath, methodName);

        assertTrue(isOriginalMethodReplaced, "Original method was not successfully replaced");
    }
}
