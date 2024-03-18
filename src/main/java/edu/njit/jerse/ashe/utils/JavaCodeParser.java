package edu.njit.jerse.ashe.utils;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.stmt.BlockStmt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
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
public final class JavaCodeParser {
    private static final Logger LOGGER = LogManager.getLogger(JavaCodeParser.class);
    private static final Pattern JavaCodeBlockPattern = Pattern.compile("```java(.*?)```", Pattern.DOTALL);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is a utility class and is not meant to be instantiated.
     * All methods are static and can be accessed without creating an instance.
     * Making the constructor private ensures that this class cannot be instantiated
     * from outside the class and helps to prevent misuse.
     * </p>
     */
    private JavaCodeParser() {
        throw new AssertionError("Cannot instantiate JavaCodeParser");
    }

    /**
     * Indicates the presence or absence of a modifier in a method signature.
     */
    public enum ModifierPresent {
        /**
         * Indicates that a modifier is present.
         */
        PRESENT,

        /**
         * Indicates that a modifier is absent.
         */
        ABSENT
    }

    /**
     * Represents the signature of a Java method.
     * This includes information about its access modifier, return type,
     * method name, and parameters.
     */
    public record MethodSignature(
            /**
             * Indicates the presence or absence of a modifier in a method signature.
             * <p>
             * Used to determine whether to add specific modifiers to a {@link MethodDeclaration}.
             * If the value is {@code ABSENT}, no modifiers are added to the method, reflecting the default
             * or package-private access. If the value is {@code PRESENT}, the associated modifiers
             * from {@code modifierKeyword()} are added to the method.
             */
            ModifierPresent modifierPresent,
            /**
             * A list of access modifiers for the method.
             * It can be empty, indicating that the method has default (package-private) access.
             */
            List<Modifier.Keyword> modifierKeyword,
            /**
             * The return type of the method.
             */
            String returnType,
            /**
             * The name of the method.
             */
            String methodName,
            /**
             * The method's parameters, formatted as a comma-separated string.
             * Each parameter comprises its type followed by its name, e.g., "int x, String y".
             * This string is typically processed further to delineate individual parameter nodes
             * when generating a {@link MethodDeclaration}.
             */
            String parameters
    ) {

        /**
         * Checks if the method signature is a valid Java method signature.
         *
         * @return {@code true} if the method signature contains non-null return type, method name,
         * and parameters, representing a valid Java method signature; {@code false} otherwise.
         */
        public boolean isValid() {
            boolean isValidSig =
                    this.returnType() != null &&
                            this.methodName() != null &&
                            this.parameters() != null;

            if (isValidSig) {
                LOGGER.info("Java method signature is valid: returnType={}, methodName={}, parameters={}",
                        this.returnType(), this.methodName(), this.parameters());
            } else {
                LOGGER.warn("Invalid Java method signature detected: returnType={}, methodName={}, parameters={}",
                        this.returnType(), this.methodName(), this.parameters());
            }

            return isValidSig;
        }
    }

    /**
     * Extracts method signature from a given method string.
     *
     * @param method the method string from which to extract the signature
     * @return {@link MethodSignature} representing the extracted method signature
     * @throws IllegalArgumentException if the {@link MethodDeclaration} is empty
     * @throws ParseProblemException    if the method cannot be parsed
     */
    public static MethodSignature extractMethodSignature(String method)
            throws IllegalArgumentException, ParseProblemException {
        try {
            CompilationUnit cu = StaticJavaParser.parse(method);
            Optional<MethodDeclaration> methodDeclarationOpt = cu.findFirst(MethodDeclaration.class);

            // Explicitly check if the method declaration is present
            if (methodDeclarationOpt.isEmpty()) {
                String errorMessage = "Invalid method string: " + method;
                LOGGER.error(errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            MethodDeclaration methodDeclaration = methodDeclarationOpt.get();
            String returnType = methodDeclaration.getType().asString();
            String name = methodDeclaration.getName().asString();
            String params = extractParameters(methodDeclaration);
            List<Modifier.Keyword> modifiers = extractModifiers(methodDeclaration);
            ModifierPresent presence = determineModifierPresence(modifiers);

            LOGGER.info("Extracted method signature: Modifiers={} ReturnType={} Name={} Parameters={}",
                    modifiers.isEmpty() ? "None" : modifiers.stream().map(Enum::name).collect(Collectors.joining(", ")),
                    returnType, name, params);

            return new MethodSignature(presence, modifiers, returnType, name, params);
        } catch (ParseProblemException ex) {
            LOGGER.error("Failed to parse method due to syntax error(s): " + ex.getProblems());
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("Failed to extract method signature due to an unexpected error: " + ex);
            throw ex;
        }
    }

    /**
     * Determines the presence of modifiers in a method signature.
     *
     * @param keywords a list of modifier keywords associated with a method
     * @return {@code ModifierPresent.ABSENT} if the {@code DEFAULT} modifier is present in the list,
     * indicating no other modifiers are to be considered. Returns {@code ModifierPresent.PRESENT} otherwise.
     */
    private static ModifierPresent determineModifierPresence(List<Modifier.Keyword> keywords) {
        return keywords.contains(Modifier.Keyword.DEFAULT) ? ModifierPresent.ABSENT : ModifierPresent.PRESENT;
    }

    /**
     * Extracts and returns the parameters from the provided method declaration
     * as a formatted string.
     *
     * @param methodDeclaration the method declaration from which to extract the parameters
     * @return a formatted string representing the parameters of the method
     */
    private static String extractParameters(MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters()
                .stream()
                .map(p -> p.getType().asString() + " " + p.getName().asString())
                .collect(Collectors.joining(", "));
    }

    /**
     * Extracts the modifier keyword(s) from a given {@link MethodDeclaration}.
     * Returns {@link Modifier.Keyword#DEFAULT} if no specific modifier is found.
     *
     * @param methodDeclaration the method declaration to extract the modifier(s) from
     * @return the extracted modifier or {@link Modifier.Keyword#DEFAULT} if none found
     */
    private static List<Modifier.Keyword> extractModifiers(MethodDeclaration methodDeclaration) {
        List<Modifier.Keyword> keywords = methodDeclaration.getModifiers()
                .stream()
                .map(Modifier::getKeyword)
                .collect(Collectors.toList());
        if (keywords.isEmpty()) {
            keywords.add(Modifier.Keyword.DEFAULT);
        }
        return keywords;
    }

    /**
     * Extracts the method name from a provided target method string.
     * <p>
     * The expected format is: "package.name.ClassName#methodName()"
     * Parameter types must always be provided, though they can be empty if the method has no parameters.
     * For example:
     * <ul>
     *     <li>"com.example.package.MyClass#myMethod(ParamType1, ParamType2)".</li>
     *     <li>"com.example.package.MyClass#myMethod()". If the method has no parameters.</li>
     * </ul>
     * <p>
     * If the target method string does not match the expected format,
     * this method throws an {@link IllegalArgumentException}.
     *
     * @param targetMethod the target method string in the format "package.name.ClassName#methodName()"
     * @return the extracted method name from the provided target method string
     * @throws IllegalArgumentException if the targetMethod format is invalid
     */
    public static String extractMethodName(String targetMethod) throws IllegalArgumentException {
        Pattern pattern = Pattern.compile("#(.*?)\\(");
        Matcher matcher = pattern.matcher(targetMethod);

        if (matcher.find()) {
            // Suppressed warning: Based on Matcher's behavior, group(1) won't be null if find() succeeds.
            // Explanation: In the context of the regex pattern "#(.*?)\\(", `group(0)` would represent the entire
            // matched substring starting from `#` and ending just before the first open parenthesis `(`.
            // So, for an input like "com.example.package.MyClass#myMethod(ParamType1, ParamType2)", `group(0)`
            // would return `#myMethod(`, while `group(1)` captures just the method name, returning `myMethod`.
            @SuppressWarnings("nullness")
            String methodName = matcher.group(1);
            if (methodName != null) {
                return methodName;
            }
        }

        throw new IllegalArgumentException("Invalid targetMethod format");
    }

    /**
     * Extracts the body of a specified method from the given Java code string.
     *
     * @param method the entire Java method as a string
     * @return the body of the method as a string
     * @throws ParseProblemException  if the method cannot be parsed
     * @throws NoSuchElementException if the method declaration or body is not found
     */
    public static String extractMethodBody(String method) throws ParseProblemException, NoSuchElementException {
        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(method);
        } catch (ParseProblemException ex) {
            LOGGER.error("Failed to parse method: " + ex.getMessage());
            throw ex;
        }

        Optional<MethodDeclaration> methodDeclarationOpt = cu.findFirst(MethodDeclaration.class);
        if (methodDeclarationOpt.isEmpty()) {
            String errorMessage = "Method declaration not found.";
            LOGGER.error(errorMessage);
            throw new NoSuchElementException(errorMessage);
        }

        MethodDeclaration methodDeclaration = methodDeclarationOpt.get();
        Optional<BlockStmt> methodBodyOpt = methodDeclaration.getBody();

        if (methodBodyOpt.isEmpty()) {
            String errorMessage = "Method body not found.";
            LOGGER.error(errorMessage);
            throw new NoSuchElementException(errorMessage);
        }

        return methodBodyOpt.get().toString();
    }

    /**
     * Finds the class or interface declaration containing a method with a specified name from a Java file.
     *
     * @param filePath   the path to the Java file
     * @param methodName the name of the method you're looking for
     * @return the name of the class or interface containing the specified method
     * @throws IOException            if the file cannot be read
     * @throws ParseProblemException  if the file cannot be parsed
     * @throws NoSuchElementException if the specified method is not found in the file
     */
    public static ClassOrInterfaceDeclaration extractClassByMethodName(String filePath, String methodName)
            throws IOException, ParseProblemException, NoSuchElementException {
        CompilationUnit cu;
        try (FileInputStream fis = new FileInputStream(filePath)) {
            cu = StaticJavaParser.parse(fis);
        } catch (IOException ex) {
            LOGGER.error("Error reading file: " + filePath + " " + ex.getMessage());
            throw ex;
        } catch (ParseProblemException ex) {
            LOGGER.error("Parse error in file: " + filePath + " " + ex.getMessage());
            throw ex;
        }

        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        Optional<ClassOrInterfaceDeclaration> classOrInterfaceOpt = classes.stream()
                .filter(declaration -> declaration
                        .getMethods()
                        .stream()
                        .anyMatch(method -> method.getNameAsString().equals(methodName)))
                .findFirst();

        if (classOrInterfaceOpt.isEmpty()) {
            String errorMessage = "No class or interface declarations containing the method " +
                    methodName + " found in file: " + filePath;
            LOGGER.error(errorMessage);
            throw new NoSuchElementException(errorMessage);
        }

        return classOrInterfaceOpt.get();
    }

    /**
     * Extracts a Java code block enclosed in {@code ```java ... ```} from a given response string.
     *
     * @param response the response string potentially containing a Java code block
     * @return the Java code block without enclosing tags, or empty string if not found
     */
    public static String extractJavaCodeBlockFromResponse(String response) {
        Matcher matcher = JavaCodeBlockPattern.matcher(response);

        if (matcher.find()) {
            String matchedGroup = matcher.group(1);
            if (matchedGroup != null) {
                LOGGER.debug("Extracted Java code block from response: {}", matchedGroup);

                // Strip comments from the extracted Java code block
                String stripCommentsPattern = "(?s)//.*?(\r?\n)|/\\*.*?\\*/";
                String strippedCodeBlock = matchedGroup.trim().replaceAll(stripCommentsPattern, "$1");
                LOGGER.info("Stripped comments from Java code block: {}", strippedCodeBlock);
                return strippedCodeBlock;
            }
        }

        return "";
    }
}
