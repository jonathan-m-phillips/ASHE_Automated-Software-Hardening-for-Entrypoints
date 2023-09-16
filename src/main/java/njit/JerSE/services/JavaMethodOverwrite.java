package njit.JerSE.services;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import njit.JerSE.utils.JavaCodeParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * Provides functionality to overwrite Java methods within a given file.
 * <p>
 * This service uses the JavaParser library to analyze and manipulate Java source files,
 * facilitating the replacement of methods with new implementations provided as input.
 */
public class JavaMethodOverwrite {

    /**
     * Writes a new method to a given Java file, replacing the existing method.
     *
     * @param filePath the path to the Java file
     * @param newMethodCode the new method code to be written
     * @return {@code true} if the operation was successful; {@code false} otherwise.
     */
    public boolean writeToFile(String filePath, String newMethodCode) {
        Path path = Paths.get(filePath);
        JavaCodeParser javaCodeParser = new JavaCodeParser();

        Optional<JavaCodeParser.MethodSignature> methodSignatureOpt = javaCodeParser.extractMethodSignature(newMethodCode);
        if (methodSignatureOpt.isEmpty() || !isValidMethodSignature(methodSignatureOpt.get())) {
            // Throughout, it might be helpful for diagnastic messages to show the problematic text.
            System.out.println("Could not extract or validate method signature from provided text.");
            return false;
        }

        CompilationUnit cu;
        try {
            cu = StaticJavaParser.parse(path);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        Optional<ClassOrInterfaceDeclaration> mainClassOpt = getMainClassFromPath(cu);
        if (mainClassOpt.isEmpty()) {
            System.out.println("No primary class found in the provided file.");
            return false;
        }

        MethodDeclaration newMethod = createNewMethodFromSignature(methodSignatureOpt.get(), javaCodeParser, newMethodCode);
        mainClassOpt.get().getMembers().clear();
        mainClassOpt.get().addMember(newMethod);

        return writeCompilationUnitToFile(path, cu);
    }

    /**
     * Validates the extracted method signature.
     *
     * @param signature the method signature to be validated
     * @return {@code true} if the method signature are valid; {@code false} otherwise.
     */
    private boolean isValidMethodSignature(JavaCodeParser.MethodSignature signature) {
        return signature.returnType() != null && signature.methodName() != null && signature.parameters() != null;
    }

    /**
     * Retrieves the primary class declaration from a given compilation unit.
     *
     * @param cu the compilation unit containing the Java source code
     * @return an optional containing the primary class declaration if found; an empty optional otherwise
     */
    private Optional<ClassOrInterfaceDeclaration> getMainClassFromPath(CompilationUnit cu) {
        return cu.getPrimaryType().flatMap(BodyDeclaration::toClassOrInterfaceDeclaration);
    }

    /**
     * Creates a new {@link MethodDeclaration} object from the provided method signature.
     *
     * @param signature the signature of the method to be created
     * @param parser the Java code parser utility
     * @param newMethodCode the new method code
     * @return the newly constructed {@link MethodDeclaration} object
     */
    private MethodDeclaration createNewMethodFromSignature(JavaCodeParser.MethodSignature signature, JavaCodeParser parser, String newMethodCode) {
        MethodDeclaration newMethod = new MethodDeclaration();
        newMethod.setType(signature.returnType());
        newMethod.setName(signature.methodName());

        String[] rawParameters = signature.parameters().split(",");
        NodeList<Parameter> parameters = new NodeList<>();
        for (String rawParam : rawParameters) {
            String[] parts = rawParam.trim().split(" ");
            if (parts.length == 2) {
                Parameter parameter = new Parameter(StaticJavaParser.parseType(parts[0]), parts[1]);
                parameters.add(parameter);
            }
        }

        newMethod.setParameters(parameters);
        newMethod.setBody(StaticJavaParser.parseBlock(parser.extractMethodBodyByName(newMethodCode, signature.methodName())));

        return newMethod;
    }

    /**
     * Writes the updated compilation unit back to the file.
     *
     * @param path the path to the Java file
     * @param cu the updated compilation unit
     * @return {@code true} if the write operation was successful; {@code false} otherwise.
     */
    private boolean writeCompilationUnitToFile(Path path, CompilationUnit cu) {
        try {
            System.out.println("Writing to file...");
            Files.write(path, cu.toString().getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

}
