package edu.njit.jerse.ashe.services;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import edu.njit.jerse.ashe.utils.JavaCodeParser;
import edu.njit.jerse.ashe.utils.JavaCodeParser.MethodSignature;
import edu.njit.jerse.ashe.utils.JavaCodeParser.ModifierPresent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Provides functionality to replace Java methods within a given file.
 * <p>
 * This service uses the JavaParser library to analyze and manipulate Java source files,
 * facilitating the replacement of methods with new implementations provided as input.
 */
public final class MethodReplacementService {
    private static final Logger LOGGER = LogManager.getLogger(MethodReplacementService.class);

    /**
     * Private constructor to prevent instantiation.
     * <p>
     * This class is not meant to be instantiated.
     * All methods are static and can be accessed without creating an instance.
     * Making the constructor private ensures that this class cannot be instantiated
     * from outside the class and helps to prevent misuse.
     * </p>
     */
    private MethodReplacementService() {
        throw new AssertionError("Cannot instantiate MethodReplacementService");
    }

    /**
     * Replaces an existing Java method in the specified file with a new, updated method.
     *
     * @param absoluteFilePath the absolute path to the Java file containing the method to be replaced
     * @param newMethodCode    the new method code to replace the existing method
     * @return {@code true} if the replacement operation was successful; {@code false} otherwise
     */
    public static boolean replaceMethodInFile(String absoluteFilePath, String className, String newMethodCode) {
        LOGGER.info("Attempting to replace method in file: {}", absoluteFilePath);

        Path path = Paths.get(absoluteFilePath);

        MethodSignature methodSignature = JavaCodeParser.extractMethodSignature(newMethodCode);
        if (!methodSignature.isValid()) {
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

        ClassOrInterfaceDeclaration classDec = getClassDeclaration(cu, className);
        MethodDeclaration newMethod = createNewMethodFromSignature(methodSignature, newMethodCode);
        boolean wasMethodReplaced = replaceMethodInClassDeclaration(classDec, newMethod, methodSignature);
        if (!wasMethodReplaced) {
            LOGGER.error("No matching method found to replace in file: {}", absoluteFilePath);
            return false;
        }

        boolean didWriteCUToFile = writeCompilationUnitToFile(path, cu);
        if (didWriteCUToFile) {
            LOGGER.info("Method replacement succeeded for file: {}", absoluteFilePath);
        } else {
            LOGGER.error("Method replacement failed for file: {}", absoluteFilePath);
        }

        return didWriteCUToFile;
    }

    /**
     * Replaces a method in the specified class declaration with a new method if it matches the provided method signature.
     *
     * @param classDecl       the class or interface declaration where the method replacement should be performed
     * @param newMethod       the new method declaration that will replace the existing method if a match is found
     * @param methodSignature the signature of the method to be replaced. Replacement is done based on this signature
     * @return {@code true} if a method with the provided signature was found and replaced; {@code false} otherwise
     */
    private static boolean replaceMethodInClassDeclaration(ClassOrInterfaceDeclaration classDecl, MethodDeclaration newMethod, MethodSignature methodSignature) {
        for (MethodDeclaration method : classDecl.getMethods()) {
            // If method has the same signature as the one we want to replace, replace it
            if (doesMethodSignatureMatch(method, methodSignature)) {
                method.replace(newMethod);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the provided method matches the target method signature based on "override-equivalent" rules
     * as defined in the Java Language Specification (JLS) - <a href="https://docs.oracle.com/javase/specs/jls/se8/html/jls-8.html#jls-8.4.2">§8.4.2</a>.
     * <p>
     * The match-check is performed based on:
     * 1. Method name.
     * 2. Number of parameters.
     * 3. Type of parameters (order matters).
     * 4. Return type - this deviates slightly from the JLS where only parameter types
     *                  are considered for "override-equivalent".
     *
     * @param method          the method declaration to check against the target signature
     * @param targetSignature the target method signature to which the provided method is compared
     * @return {@code true} if the method matches the target signature; {@code false} otherwise
     * <p>
     * Example Usage:
     * doesMethodSignatureMatch(someMethodDecl, new MethodSignature("methodName", "paramType1, paramType2", "returnType"));
     */
    // TODO: Parameter names and parameter default values/annotations are not considered during
    // TODO: the match. The handling of type variables needs to be checked.
    private static boolean doesMethodSignatureMatch(MethodDeclaration method, MethodSignature targetSignature) {
        // Check if the method name matches
        if (!method.getNameAsString().equals(targetSignature.methodName())) {
            return false;
        }

        // Split the parameters string into an array and check if the parameter count matches
        List<String> targetParameterTypes = splitParameters(targetSignature.parameters());
        boolean noParameters = targetParameterTypes.size() == 1 && targetParameterTypes.get(0).isEmpty();

        if (noParameters && method.getParameters().isEmpty()) {
            // If both method and target have no parameters, they match
            return method.getTypeAsString().equals(targetSignature.returnType());
        } else if (method.getParameters().size() != targetParameterTypes.size()) {
            // If parameter counts (excluding the no parameter case) don't match, return false
            return false;
        }

        // Check if the parameter types match
        for (int i = 0; i < method.getParameters().size(); i++) {
            String actualParamType = method.getParameter(i).getType().asString();
            String expectedParamType = targetParameterTypes.get(i).split(" ")[0]; // Only compare type, not name

            if (!actualParamType.equals(expectedParamType)) {
                return false;
            }
        }

        return method.getTypeAsString().equals(targetSignature.returnType());
    }

    /**
     * Splits a parameter string into individual parameter definitions, taking into account nested
     * generic types. This method is designed to handle complex type definitions, such as {@code Map<String, Integer>}.
     * <p>
     * The method ensures that the split occurs only at the top-level commas, avoiding splits inside generic definitions.
     *
     * @param parameterString the entire parameter string that needs to be split into individual parameter definitions
     * @return a list of individual parameter definitions split from the input string
     */
    private static List<String> splitParameters(String parameterString) {
        if (parameterString == null || parameterString.isEmpty()) {
            // returns an empty list if the parameter string is null or empty
            return Collections.emptyList();
        }

        List<String> result = new ArrayList<>();
        int depth = 0;
        StringBuilder currentParameter = new StringBuilder();

        for (char c : parameterString.toCharArray()) {
            switch (c) {
                case '<':
                    depth++;
                    break;
                case '>':
                    depth--;
                    break;
                case ',':
                    if (depth == 0) {
                        result.add(currentParameter.toString().trim());
                        currentParameter = new StringBuilder();
                        continue;
                    }
                    break;
            }
            currentParameter.append(c);
        }
        if (!currentParameter.isEmpty()) {
            result.add(currentParameter.toString().trim());
        }

        return result;
    }

    /**
     * Retrieves the declaration of a specific class or interface by name from the
     * provided compilation unit.
     *
     * @param cu        the compilation unit from which the class or interface
     *                  declaration needs to be extracted
     * @param className the name of the class or interface whose declaration is
     *                  to be fetched
     * @return the declaration of the target class or interface
     * @throws IllegalArgumentException if the target class declaration is not found
     */
    private static ClassOrInterfaceDeclaration getClassDeclaration(CompilationUnit cu, String className)
            throws IllegalArgumentException {
        List<ClassOrInterfaceDeclaration> classes = cu.findAll(ClassOrInterfaceDeclaration.class);

        for (ClassOrInterfaceDeclaration classOrInterface : classes) {
            if (classOrInterface.getNameAsString().equals(className)) {
                LOGGER.debug("Retrieved the targeted class declaration: {}", className);
                return classOrInterface;
            }
        }

        String errorMessage = "The targeted class declaration was not found in the provided compilation unit.";
        LOGGER.warn(errorMessage);
        throw new IllegalArgumentException(errorMessage);
    }

    /**
     * Creates a new {@link MethodDeclaration} object from the provided method signature.
     *
     * @param signature     the signature of the method to be created
     * @param newMethodCode the new method code
     * @return the newly constructed {@link MethodDeclaration} object
     * @throws IllegalArgumentException if the parameter format is invalid
     */
    private static MethodDeclaration createNewMethodFromSignature(MethodSignature signature, String newMethodCode)
            throws IllegalArgumentException {
        LOGGER.info("Creating a new method from the provided signature.");

        MethodDeclaration newMethod = new MethodDeclaration();

        if (signature.modifierPresent() != ModifierPresent.ABSENT) {
            signature.modifierKeyword().forEach(newMethod::addModifier);
        }

        newMethod.setType(signature.returnType());
        newMethod.setName(signature.methodName());

        LOGGER.debug("Set method name to '{}' and return type to '{}'.", signature.methodName(), signature.returnType());

        List<String> rawParameters = splitParameters(signature.parameters());
        boolean isRawParamsEmpty = rawParameters.stream().anyMatch(param -> param.trim().isEmpty());

        // If the method signature has parameters,
        // parse them and add them to the new method.
        // Otherwise, skip and set the method body
        if (!isRawParamsEmpty) {
            NodeList<Parameter> parameters = new NodeList<>();

            for (String rawParam : rawParameters) {
                String[] parts = rawParam.trim().split(" ");

                if (parts.length >= 2) {
                    List<String> typeParts = Arrays.stream(Arrays.copyOf(parts, parts.length - 1))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());
                    String typeName = String.join(" ", typeParts);
                    String paramName = parts[parts.length - 1];

                    Parameter parameter = new Parameter(StaticJavaParser.parseType(typeName), paramName);
                    parameters.add(parameter);
                    LOGGER.debug("Added parameter of type '{}' with name '{}'.", typeName, paramName);
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

        newMethod.setBody(StaticJavaParser.parseBlock(JavaCodeParser.extractMethodBody(newMethodCode)));
        LOGGER.debug("Set method body.");

        return newMethod;
    }

    /**
     * Writes the updated compilation unit back to the file.
     *
     * @param path the path to the Java file
     * @param cu   the updated compilation unit
     * @return {@code true} if the write operation was successful; {@code false} otherwise
     */
    private static boolean writeCompilationUnitToFile(Path path, CompilationUnit cu) {
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

    /**
     * Replaces the original method in the target file with a checked (modified) method.
     *
     * @param checkedFile the path to the Java file containing the checked (Checker Framework compiled) method
     * @param targetFile  the path to the Java file containing the original method to be replaced
     * @return true if the method replacement was successful; false otherwise
     * @throws IOException            if an IO error occurs while extracting the class from the checked file
     * @throws NoSuchElementException if the specified method is not found in the file
     * @throws RuntimeException       if the original method cannot be replaced in the target file
     */
    public static boolean replaceOriginalTargetMethod(String checkedFile, String targetFile, String methodName)
            throws IOException, NoSuchElementException, RuntimeException {
        ClassOrInterfaceDeclaration checkedClass = JavaCodeParser.extractClassByMethodName(checkedFile, methodName);

        boolean wasOriginalMethodReplaced = replaceMethodInFile(targetFile, checkedClass.getNameAsString(), checkedClass.toString());
        if (!wasOriginalMethodReplaced) {
            String errorMessage = "Failed to replace the original method in the target file.";
            LOGGER.error(errorMessage);
            throw new RuntimeException(errorMessage);
        }

        LOGGER.info("Original method in the target file replaced successfully.");
        return true;
    }
}
