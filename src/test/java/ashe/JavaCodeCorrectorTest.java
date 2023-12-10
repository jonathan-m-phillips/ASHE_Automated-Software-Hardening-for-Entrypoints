package ashe;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import edu.njit.jerse.ashe.services.CheckerFrameworkCompiler;
import edu.njit.jerse.ashe.services.MethodReplacementService;
import edu.njit.jerse.ashe.services.SpeciminTool;
import edu.njit.jerse.ashe.utils.JavaCodeParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

class JavaCodeCorrectorTest {
    private static final Pattern TARGET_FILE_PATTERN = Pattern.compile("([a-zA-Z_0-9]+/)*[a-zA-Z_0-9]+\\.java");
    private static final Pattern TARGET_METHOD_PATTERN = Pattern.compile("[a-zA-Z_0-9]+(\\.[a-zA-Z_0-9]+)*#[a-zA-Z_0-9]+\\([^\\)]*\\)");

    private final String responseContent;

    public JavaCodeCorrectorTest(String responseFilePath) {
        this.responseContent = loadResponsesFromFile(responseFilePath);
    }

    private static String loadResponsesFromFile(String filePath) {
        StringBuilder contentBuilder = new StringBuilder();
        try {
            List<String> lines = Files.readAllLines(Paths.get(filePath));
            for (String line : lines) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return contentBuilder.toString();
    }

    public boolean fixTargetFileErrorsWithGptSimulation(String targetFile, String targetMethod)
            throws IOException, IllegalArgumentException {

        String errorOutput = checkedFileError(targetFile);
        if (errorOutput.isEmpty()) {
            return false;
        }

        while (!errorOutput.isEmpty()) {
            String methodName = JavaCodeParser.extractMethodName(targetMethod);
            ClassOrInterfaceDeclaration checkedClass = JavaCodeParser.extractClassByMethodName(targetFile, methodName);

            String gptCorrection = JavaCodeParser.extractJavaCodeBlockFromResponse(this.responseContent);

            boolean wasMethodReplaced = MethodReplacementService.replaceMethodInFile(targetFile, checkedClass.getNameAsString(), gptCorrection);
            if (!wasMethodReplaced) {
                return false;
            }

            errorOutput = checkedFileError(targetFile);
        }

        return true;
    }

    public String minimizeTargetFile(String root, String targetFile, String targetMethod)
            throws IOException, InterruptedException {
        if (!isValidTargetFileFormat(targetFile)) {
            throw new RuntimeException("Formatting error: targetFile does not adhere to the required format.");
        }

        String adjustedTargetMethod = ensureWhitespaceAfterCommas(targetMethod);
        if (!isValidTargetMethodFormat(adjustedTargetMethod)) {
            throw new RuntimeException("Formatting error: targetMethod does not adhere to the required format.");
        }

        String minimizedDirectory = SpeciminTool.runSpeciminTool(root, targetFile, adjustedTargetMethod);

        if (minimizedDirectory.isEmpty()) {
            throw new RuntimeException("Specimin tool failed to run or did not produce an output directory.");
        }

        return minimizedDirectory;
    }

    public String checkedFileError(String targetFile) {
        String errorOutput;

        try {
            errorOutput = CheckerFrameworkCompiler.compileWithCheckerFramework(targetFile);
            return errorOutput;
        } catch (IOException e) {
            // Return an empty string to indicate that
            // no errors were found in the checked file
            return "";
        }
    }

    private static boolean isValidTargetFileFormat(String targetFile) {
        return TARGET_FILE_PATTERN.matcher(targetFile).matches();
    }

    private static boolean isValidTargetMethodFormat(String targetMethod) {
        return TARGET_METHOD_PATTERN.matcher(targetMethod).matches();
    }

    public static String ensureWhitespaceAfterCommas(String input) {
        if (input.contains(",") && !input.contains(", ")) {
            return input.replaceAll(",(?! )", ", ");
        }
        return input;
    }
}