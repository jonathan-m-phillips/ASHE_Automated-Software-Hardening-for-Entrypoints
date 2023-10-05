package njit.JerSE.services;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import njit.JerSE.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Optional;

// TODO: this class has no fields, so it would be better to make it non-instantiable
// (by making its no-arguments constructor private) and then making all of its methods
// static.
/**
 * Provides functionality to replace Java methods within a given file.
 * <p>
 * This service uses the JavaParser library to analyze and manipulate Java source files,
 * facilitating the replacement of methods with new implementations provided as input.
 */
public class MethodReplacementService {
    private static final Logger LOGGER = LogManager.getLogger(MethodReplacementService.class);

    /**
     * Replaces an existing Java method in the specified file with a new, updated method body.
     * The signature of the new method must be identical to the signature of the old method.
     *
     * @param filePath      the (TODO: absolute or relative?) path to the Java file containing the method to be replaced.
     * @param newMethodCode the new method code to replace the existing method (including both
     *                      the method's signature and its body, exactly as it would appear
     *                      in a Java source file).
     * @return {@code true} if the replacement operation was successful; {@code false} otherwise.
     */
    public boolean replaceMethod(String filePath, String newMethodCode) {
        LOGGER.info("Attempting to replace method in file: {}", filePath);

        Path path = Paths.get(filePath);
        JavaCodeParser javaCodeParser = new JavaCodeParser();

        Optional<JavaCodeParser.MethodSignature> methodSignatureOpt = javaCodeParser.extractMethodSignature(newMethodCode);
        if (methodSignatureOpt.isEmpty() || !isValidMethodSignature(methodSignatureOpt.get())) {
            LOGGER.error("Could not parse the provided method.");
            return false;
        }

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(path);
        } catch (Exception ex) {
            String errorMessage = (ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
            LOGGER.error("Error while parsing file {}: {}", path, errorMessage);
            return false;
        }

        Optional<ClassOrInterfaceDeclaration> mainClassOpt = getMainClass(cu);
        if (mainClassOpt.isEmpty()) {
            LOGGER.error("No class found in {}", path);
            return false;
        }

        MethodDeclaration newMethod = createNewMethodFromSignature(methodSignatureOpt.get(), javaCodeParser, newMethodCode);
        mainClassOpt.get().getMembers().clear();
        mainClassOpt.get().addMember(newMethod);

        boolean didWriteCUToFile = writeCompilationUnitToFile(path, cu);
        if (didWriteCUToFile) {
            LOGGER.info("Method replacement succeeded for file: {}", filePath);
        } else {
            LOGGER.error("Method replacement failed for file: {}", filePath);
        }
        return didWriteCUToFile;
    }

    /**
     * Checks if the extracted method signature is both complete and conforms to the expected
     * format of a valid Java method signature.
     * TODO: is it possible for the signature not to have the expected format? The method's implementation
     * appears to only check for completeness.
     *
     * @param signature the method signature to be checked
     * @return {@code true} if the method signature is complete and a valid Java method signature; {@code false} otherwise.
     */
    // TODO: this is more of an opinion, but I strongly prefer to import inner classes directly
    // rather than using the Outer.Inner format (as you've done for the parameter on the next line).
    // I prefer this format because I find it makes the code more readable; I only use the Outer.Inner
    // format when the inner class name is so generic that it doesn't make sense on its own, such as
    // Map.Entry or Tree.Kind.
    private boolean isValidMethodSignature(JavaCodeParser.MethodSignature signature) {
        boolean isValidSig =
                signature.returnType() != null &&
                        signature.methodName() != null &&
                        signature.parameters() != null;

        if (isValidSig) {
            LOGGER.debug("Java method signature is valid: returnType={}, methodName={}, parameters={}",
                    signature.returnType(), signature.methodName(), signature.parameters());
        } else {
            LOGGER.warn("Invalid Java method signature detected: returnType={}, methodName={}, parameters={}",
                    signature.returnType(), signature.methodName(), signature.parameters());
        }

        return isValidSig;
    }

    /**
     * Retrieves the primary class declaration from a given compilation unit.
     *
     * @param cu the compilation unit containing the Java source code
     * @return an optional containing the primary class declaration if found; an empty optional otherwise
     */
    private Optional<ClassOrInterfaceDeclaration> getMainClass(CompilationUnit cu) {
        Optional<ClassOrInterfaceDeclaration> mainClassOpt = cu.getPrimaryType().flatMap(BodyDeclaration::toClassOrInterfaceDeclaration);

        if (mainClassOpt.isPresent()) {
            LOGGER.debug("Retrieved main class declaration: {}", mainClassOpt.get().getNameAsString());
        } else {
            LOGGER.warn("No main class declaration found in the provided compilation unit.");
        }

        return mainClassOpt;
    }

    /**
     * Creates a new {@link MethodDeclaration} object from the provided method signature.
     *
     * @param signature     the signature of the method to be created
     * @param parser        the Java code parser
     * @param newMethodCode the new method code
     * @return the newly constructed {@link MethodDeclaration} object
     */
    private MethodDeclaration createNewMethodFromSignature(JavaCodeParser.MethodSignature signature, JavaCodeParser parser, String newMethodCode) {
        LOGGER.info("Creating a new method from the provided signature.");

        MethodDeclaration newMethod = new MethodDeclaration();
        newMethod.setType(signature.returnType());
        newMethod.setName(signature.methodName());

        LOGGER.debug("Set method name to '{}' and return type to '{}'.", signature.methodName(), signature.returnType());

        String[] rawParameters = signature.parameters().split(",");
        boolean isRawParamsEmpty = Arrays.stream(rawParameters).anyMatch(param -> param.trim().isEmpty());

        // If the method signature has parameters,
        // parse them and add them to the new method.
        // Otherwise, skip and set the method body
        if (!isRawParamsEmpty) {
            NodeList<Parameter> parameters = new NodeList<>();

            for (String rawParam : rawParameters) {
                String[] parts = rawParam.trim().split(" ");

                if (parts.length == 2) {
                    Parameter parameter = new Parameter(StaticJavaParser.parseType(parts[0]), parts[1]);
                    parameters.add(parameter);
                    LOGGER.debug("Added parameter of type '{}' with name '{}'.", parts[0], parts[1]);
                } else {
                    LOGGER.error("Invalid parameter format encountered: '{}'. Throwing exception.", rawParam);
                    throw new IllegalArgumentException("Invalid parameter: " + rawParam);
                }
            }
            newMethod.setParameters(parameters);
            LOGGER.debug("All parameters set for the method.");
        } else {
            LOGGER.debug("No parameters provided for the method.");
        }

        newMethod.setBody(StaticJavaParser.parseBlock(parser.extractMethodBody(newMethodCode)));
        LOGGER.debug("Set method body.");

        return newMethod;
    }

    /**
     * Writes the updated compilation unit back to the file.
     *
     * @param path the path to the Java file
     * @param cu   the updated compilation unit
     * @return {@code true} if the write operation was successful; {@code false} otherwise.
     */
    private boolean writeCompilationUnitToFile(Path path, CompilationUnit cu) {
        try {
            LOGGER.debug("Writing updated compilation unit to file...");
            Files.write(path, cu.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException ex) {
            String errorMessage = (ex.getMessage() != null) ? ex.getMessage() : "Unknown error";
            LOGGER.error("Error writing to file {}: {}", path, errorMessage);
            return false;
        }
    }
}
