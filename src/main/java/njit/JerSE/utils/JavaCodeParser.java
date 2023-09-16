package njit.JerSE.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class to parse and analyze Java code using the JavaParser library.
 * <p>
 * This class provides methods for extracting method signature, method bodies,
 * class declarations, and Java code blocks from a given string or file.
 */
public class JavaCodeParser {

    /**
     * Represents the signature of a Java method: its return type,
     * method name, and parameters.
     */
    public record MethodSignature(String returnType, String methodName, String parameters) {
    }

    /**
     * Extracts method signature from a given method string.
     *
     * @param method the method as a string
     * @return an Optional containing {@link MethodSignature} if found, else empty
     */
    public Optional<MethodSignature> extractMethodSignature(String method) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(method);

            // Find the first method in the parsed code
            Optional<MethodDeclaration> methodDeclarationOpt = cu.findFirst(MethodDeclaration.class);

            if (methodDeclarationOpt.isPresent()) {
                MethodDeclaration methodDeclaration = methodDeclarationOpt.get();

                // Get return type
                String returnType = methodDeclaration.getType().asString();

                // Get method name
                String methodName = methodDeclaration.getName().asString();

                // Get method parameters
                String parameters = methodDeclaration.getParameters()
                        .stream()
                        .map(p -> p.getType() + " " + p.getName())
                        .collect(Collectors.joining(", "));

                return Optional.of(new MethodSignature(returnType, methodName, parameters));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty(); // return an empty Optional if no value is present
    }

    /**
     * Extracts the body of a specified method from the given Java code string.
     *
     * @param method the entire Java method as a string
     * @param methodName the name of the method to find
     * @return the body of the method as a string, or empty string if not found
     */
    public String extractMethodBodyByName(String method, String methodName) {
        CompilationUnit cu = StaticJavaParser.parse(method);
        MethodDeclaration testSocketMethod = cu.findFirst(
                MethodDeclaration.class,
                mthd -> mthd.getName().asString().equals(methodName)
        ).orElse(null);

        if (testSocketMethod != null && testSocketMethod.getBody().isPresent()) {
            return testSocketMethod.getBody().get().toString();
        } else {
            System.out.println(methodName + " method not found.");
        }
        return "";
    }

    /**
     * Extracts the first class or interface declaration from a Java file.
     *
     * @param filePath the path to the Java file
     * @return the first class or interface declaration from the file
     * @throws FileNotFoundException If the file cannot be read
     */
    public ClassOrInterfaceDeclaration extractClassFromFile(String filePath) throws FileNotFoundException {
        // Use try-with-resources to ensure FileInputStream gets closed
        try (FileInputStream fis = new FileInputStream(filePath)) {
            CompilationUnit cu = StaticJavaParser.parse(fis);

            // Get all the class and interface declarations from the file
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

            return classes.get(0);
        } catch (IOException e) {
            throw new FileNotFoundException("Error reading file: " + e.getMessage());
        }
    }

    /**
     * Extracts a Java code block enclosed in {@code ```java ... ```} from a given response string.
     *
     * @param response the response string potentially containing a Java code block
     * @return the Java code block without enclosing tags, or empty string if not found
     */
    public String extractJavaCodeBlockFromResponse(String response) {
        Pattern pattern = Pattern.compile("```java(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String matchedGroup = matcher.group(1);
            if (matchedGroup != null) {
                return matchedGroup.trim();
            }
        }

        return "";
    }
}
