package njit.JerSE.utils;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

    // TODO: Why not make this Pattern static? That would enable you to 1) make this class non-instantiable,
    // and 2) convert all of its methods to static functions, which I think makes more sense for a utility
    // class.
    private final Pattern javaCodeBlockPattern;
    private static final Logger LOGGER = LogManager.getLogger(JavaCodeParser.class);

    public JavaCodeParser() {
        this.javaCodeBlockPattern = Pattern.compile("```java(.*?)```", Pattern.DOTALL);
        LOGGER.info("JavaCodeParser initialized");
    }

    /**
     * Represents the signature of a Java method: its return type,
     * method name, and parameters.
     */
    public record MethodSignature(
            String returnType,
            String methodName,
            String parameters
    ) {
    }

    /**
     * Extracts method signature from a given method string.
     *
     * @param method the method as a string
     * @return an Optional containing {@link MethodSignature} if found, else empty
     */
    // TODO: I prefer using null to indicate the lack of a value rather than Optional.
    // For a discussion of why, see Mike's article, which does a better job of explaining
    // why than I possibly could: https://homes.cs.washington.edu/~mernst/advice/nothing-is-better-than-optional.html
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

                LOGGER.debug("Extracted method signature: ReturnType={} MethodName={} Parameters={}", returnType, methodName, parameters);
                return Optional.of(new MethodSignature(returnType, methodName, parameters));
            }
            // TODO: it's good practice to try to limit the scope of try-blocks as much as possible,
            // and have different error messages/logging/catch blocks for different kinds of problems
            // that might occur.
        } catch (Exception ex) {
            LOGGER.error("Failed to extract method signature: ", ex);
        }
        return Optional.empty(); // return an empty Optional if no value is present
    }

    /**
     * Extracts the body of a specified method from the given Java code string.
     *
     * @param method the entire Java method as a string
     * @return the body of the method as a string, or an empty string if not found
     */
    public String extractMethodBody(String method) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(method);
            MethodDeclaration methodDeclaration = cu.findFirst(MethodDeclaration.class).orElse(null);

            if (methodDeclaration != null && methodDeclaration.getBody().isPresent()) {
                return methodDeclaration.getBody().get().toString();
            } else {
                LOGGER.warn("Method body not found.");
            }

        } catch (Exception ex) {
            LOGGER.error("Failed to extract method body: ", ex);
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
    public ClassOrInterfaceDeclaration extractFirstClassFromFile(String filePath) throws FileNotFoundException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            CompilationUnit cu = StaticJavaParser.parse(fis);
            List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

            if (!classes.isEmpty()) {
                return classes.get(0);
            } else {
                LOGGER.warn("No class or interface declarations found in file: {}", filePath);
                throw new ClassNotFoundException("No class or interface declarations found in file: " + filePath);
            }

        } catch (IOException ex) {
            LOGGER.error("Error reading file: {}", filePath, ex);
            throw new FileNotFoundException("Error reading file: " + ex.getMessage());
        } catch (ClassNotFoundException ex) {
            LOGGER.error("No class or interface declarations found in file: {}", filePath, ex);
            throw new RuntimeException(ex);
        }
    }

    /**
     * Extracts a Java code block enclosed in {@code ```java ... ```} from a given response string.
     *
     * @param response the response string potentially containing a Java code block
     * @return the Java code block without enclosing tags, or empty string if not found
     * TODO: I think returning the empty string here when the response can't be parsed could
     * lead to errors later, because failing to find a Java code block in a response to an ASHE-prompt
     * from GPT is an error-condition. So, I'd return null or (better) throw an exception if this
     * method fails, so that callers are forced to deal with that situation (presumably by re-invoking
     * the GPT API?).
     */
    public String extractJavaCodeBlockFromResponse(String response) {
        Matcher matcher = javaCodeBlockPattern.matcher(response);

        if (matcher.find()) {
            String matchedGroup = matcher.group(1);
            if (matchedGroup != null) {
                LOGGER.debug("Extracted Java code block from response: {}", matchedGroup);
                return matchedGroup.trim();
            }
        }

        return "";
    }
}
