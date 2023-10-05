// TODO: package names in Java are, by convention, supposed to be "reverse URLs". NJIT's
// web address is njit.edu, so a corresponding software package would be named edu.njit...
// Further, the convention in Java is to use all-lowercase. So, we should change the package
// names of all of the classes in this project so that they are prefixed by "edu.njit.jerse".
package njit.JerSE;

import njit.JerSE.services.CheckerFrameworkCompiler;
import njit.JerSE.services.GPTApiClient;
import njit.JerSE.services.MethodReplacementService;
import njit.JerSE.utils.Configuration;
import njit.JerSE.utils.JavaCodeParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * ASHE provides a means to interact with GPT to get suggestions for
 * correcting Java code. The {@link #fixJavaCodeUsingGPT(String)} method
 * is the main entry point for ASHE.
 * <p>
 * This class utilizes the Checker Framework to compile Java code and detect
 * errors. If errors are found, it fetches suggestions from GPT to fix
 * those errors, recompiles the code and overwrites the original code if
 * the suggestions result in code that compiles without errors using the
 * Checker Framework.
 */
public class ASHE {
    private static final Logger LOGGER = LogManager.getLogger(ASHE.class);

    /**
     * Initializes the configuration settings for accessing GPT API and
     * setting up prompts.
     */
    Configuration config = Configuration.getInstance();
    private final String PROMPT_START = config.getPropertyValue("gpt.prompt.start");
    private final String PROMPT_END = config.getPropertyValue("gpt.prompt.end");

    // TODO: a "Java class file" is a technical term for the output of the Java compiler,
    // which is not what you mean here (I think): `sourceFile` is actually a Java source
    // file (i.e., a .java file).
    // TODO: what about programs with more than one source file? Even after applying Specimin,
    // most programs will require multiple source files (though maybe we only need to pass
    // one of them to GPT?).
    // TODO: whenever you mention a path, you should say whether it is a relative path, and absolute
    // path, or both. It might be better to change the parameter's type from String to Path
    // to avoid this issue.
    /**
     * Fixes Java code using suggestions from GPT.
     * <p>
     *
     * @param sourceFile the path to the Java class file to be corrected
     * @throws IOException              if there's an issue accessing the file or writing to it
     * @throws FileNotFoundException    if the provided file path does not point to a valid file
     * @throws IllegalArgumentException if the GPT response is unexpected
     * @throws InterruptedException     if the API call is interrupted
     */
    // TODO: I prefer all methods to have an access modifier: the "no access modifier = access only
    // from the same package" design of Java is confusing. When I actually intend that, I usually add
    // a comment like the one below (although I suspect that this method should be public rather than
    // package-private.
    /* package-private */ void fixJavaCodeUsingGPT(String sourceFile) throws IOException, FileNotFoundException, IllegalArgumentException, InterruptedException, ExecutionException, TimeoutException {
        MethodReplacementService methodReplacement = new MethodReplacementService();
        JavaCodeParser extractor = new JavaCodeParser();
        String errorOutput = CheckerFrameworkCompiler.compile(sourceFile);
        GPTApiClient gptApiClient = new GPTApiClient();

        if (errorOutput.isEmpty()) {
            LOGGER.info("No errors found in the file.");
            return;
        }

        LOGGER.warn("Errors found in the file:" + System.lineSeparator() + errorOutput);

        while (!errorOutput.isEmpty()) {

            // TODO: this will fail if the input file contains more than one file. It might be
            // better to use something similar to Specimin's TargetMethodFinderVisitor class to
            // locate the target method more reliably. If nothing else, this method's documentation
            // should explain this limitation: if an error is issued in a method in an inner class,
            // for example, ASHE's prompt to GPT will be wrong.
            String methodWithError = String.valueOf(extractor.extractFirstClassFromFile(sourceFile));

            String prompt = methodWithError +
                    "\n" +
                    PROMPT_START +
                    "\n" +
                    errorOutput +
                    "\n" +
                    PROMPT_END;

            String gptCorrection = gptApiClient.fetchGPTResponse(prompt);
            String codeBlock = extractor.extractJavaCodeBlockFromResponse(gptCorrection);

            if (codeBlock.isEmpty()) {
                LOGGER.error("Could not extract code block from GPT response.");
                return;
            }

            LOGGER.info("Code block extracted from GPT response:" + System.lineSeparator() + codeBlock);

            if (!methodReplacement.replaceMethod(sourceFile, codeBlock)) {
                LOGGER.error("Failed to write code to file.");
                return;
            }

            LOGGER.info("File written successfully. Recompiling with Checker Framework to check for additional warnings...");

            // This will be checked at the start of the next iteration
            errorOutput = CheckerFrameworkCompiler.compile(sourceFile);

            if (!errorOutput.isEmpty()) {
                LOGGER.warn("Additional error(s) found after recompiling:" + System.lineSeparator() + errorOutput);
            }
        }

        LOGGER.info("No more errors found in the file.");
        LOGGER.info("Exiting...");
    }
}