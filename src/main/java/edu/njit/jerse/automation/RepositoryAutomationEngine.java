package edu.njit.jerse.automation;

import edu.njit.jerse.ashe.Ashe;
import edu.njit.jerse.ashe.llm.openai.models.GptModel;
import edu.njit.jerse.ashe.utils.ModelValidator;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * The {@code RepositoryAutomationEngine} class clones or fetches repositories
 * listed in a CSV file.  The {@link AsheAutomation} process is run on
 * every Java file within the repositories.
 */
public class RepositoryAutomationEngine {

    private static final Logger LOGGER = LogManager.getLogger(RepositoryAutomationEngine.class);

    /**
     * A record to store repository information.
     */
    private record Repository(
            /** The URL of the repository. */
            String url,
            /** The branch of the repository to be cloned or fetched. Null means to use the default branch (often "main" or "master"). */
            @Nullable String branch
    ) {
        Repository {
            if (url == null || url.isEmpty()) {
                throw new IllegalArgumentException("Repository URL must not be null or empty.");
            }
        }
    }

    /**
     * Processes repositories listed in the given CSV file. This method
     * takes each entry in the CSV file, clones or fetches the corresponding repository,
     * scans it for Java project roots, and runs
     * {@link AsheAutomation} to process the Java files within each project.
     * <p>
     * The format of the CSV file is as follows:
     * <pre>
     *     Repository, Branch
     *     https://url-to-your-repository.git, your-branch-to-clone
     * </pre>
     * Each line in the CSV should contain exactly these two columns, in the given order:
     * <ul>
     *     <li><b>Repository:</b> The URL of the Git repository (ending in '.git')</li>
     *     <li><b>Branch:</b> The name of the branch in the repository to be cloned or fetched</li>
     * </ul>
     * </p>
     *
     * @param csvFilePath path to the CSV file containing repository URLs and branch names,
     *                    in the format specified above.
     * @param repoDir     directory where the repositories will be cloned or fetched
     */
    private static void readAndProcessRepositoriesCsv(String csvFilePath, String repoDir, String model) {
        LOGGER.info("Starting to process CSV file: {}", csvFilePath);
        try {
            List<Repository> repositories = readRepositoriesFromCsv(csvFilePath);
            processAllRepositories(repositories, repoDir, model);
        } catch (Exception e) {
            LOGGER.error("Error processing CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error processing CSV file: " + csvFilePath, e);
        }
        LOGGER.info("Completed processing CSV file: {}", csvFilePath);
    }

    /**
     * Reads a CSV file containing repository information and converts it into a list of
     * {@link Repository} records. The CSV file must have a header specifying the "Repository"
     * and "Branch" columns. This method sets up the CSV format to skip the header record
     * during parsing.
     *
     * @param csvFilePath the file path to the CSV file that contains the repository information
     * @return a list of maps where each map contains the 'Repository' URL and the 'Branch' name
     * @throws RuntimeException if an error occurs while trying to read or parse the CSV file. The original
     *                          IOException is encapsulated within this RuntimeException.
     */
    private static List<Repository> readRepositoriesFromCsv(String csvFilePath) {
        LOGGER.info("Reading CSV file: {}", csvFilePath);
        List<Repository> repositories = new ArrayList<>();

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build();

        try (FileReader reader = new FileReader(csvFilePath);
             CSVParser csvParser = new CSVParser(reader, csvFormat)) {

            for (CSVRecord csvRecord : csvParser) {
                Repository repoInfo = new Repository(csvRecord.get("Repository"), csvRecord.get("Branch"));
                repositories.add(repoInfo);
                LOGGER.info("Added repository: {}", repoInfo);
            }
        } catch (IOException e) {
            LOGGER.error("Error reading CSV file: {}", csvFilePath, e);
            throw new RuntimeException("Error reading CSV file: " + csvFilePath, e);
        }
        return repositories;
    }

    /**
     * Processes all repositories specified in the list. Each repository is cloned or fetched,
     * then individually processed with {@link #processSingleRepository(Path, String, String)}.
     *
     * @param repositories a list of {@code Repository} records
     * @param repoDir      the directory where the repositories will be cloned or fetched
     * @param model        the large language model to use
     * @throws GitAPIException if a Git-specific error occurs during the cloning or fetching of the repositories
     * @throws IOException     if an I/O error occurs during the access or manipulation of the repository files
     */
    private static void processAllRepositories(List<Repository> repositories, String repoDir, String model)
            throws GitAPIException, IOException {
        int numOfRepositories = repositories.size();
        LOGGER.info("Starting to process {} repositories", numOfRepositories);
        for (Repository repository : repositories) {
            String repoUrl = repository.url();
            String branch = repository.branch();

            Path repoPath = cloneOrFetchRepository(repoUrl, branch, repoDir);
            processSingleRepository(repoPath, branch, model);
        }
        LOGGER.info("Completed processing {} repositories", numOfRepositories);
    }

    /**
     * Clones a repository from the given URL or fetches updates if the repository
     * already exists locally.
     *
     * @param repoUrl URL of the repository to be cloned or fetched
     * @param branch  the branch to be checked out or updated
     * @param repoDir base directory for storing cloned repositories
     * @return path to the local repository
     * @throws GitAPIException if any error occurs during cloning or fetching the repository
     * @throws IOException     if any error occurs during the cloning or fetching of the repository
     */
    public static Path cloneOrFetchRepository(String repoUrl, @Nullable String branch, String repoDir)
            throws GitAPIException, IOException {
        LOGGER.info("Attempting to clone or fetch repository: {}", repoUrl);

        String repoName = extractRepoName(repoUrl);
        Path repoPath = Paths.get(repoDir, repoName).normalize();
        if (Files.exists(repoPath)) {
            GitUtils.fetchRepository(repoPath);
        } else {
            GitUtils.cloneRepository(repoUrl, branch, repoPath);
        }
        return repoPath;
    }

    /**
     * Processes a given repository by applying the {@link AsheAutomation}
     * process to each Java project root. The method supports processing
     * multiple projects within a single repository.
     *
     * @param repoPath path to the local repository
     * @param branch   the branch of the repository to be processed
     * @throws IOException if an I/O error occurs while accessing the file system
     */
    @SuppressWarnings("nullness:argument")  // Log4J needs to be annotated for nullness
    private static void processSingleRepository(Path repoPath, @Nullable String branch, String model) throws IOException {
        LOGGER.info("Processing repository at: {} for branch: {}", repoPath, branch);

        List<Path> projectRoots = ProjectRootFinder.findJavaRoots(repoPath);
        for (Path projectRoot : projectRoots) {
            LOGGER.info("Processing project root: " + projectRoot.toString());
            AsheAutomation.processAllJavaFiles(projectRoot, projectRoot.toString(), model);
        }

        LOGGER.info("Completed processing repository at: {} for branch: {}", repoPath, branch);
    }

    /**
     * Extracts the repository name from the URL. The repository name is the substring after the last '/'
     * and before an optional '.git' suffix.
     *
     * @param repoUrl URL of the repository
     *                Example: https://github.com/your-username/your-repository.git
     * @return name of the repository extracted from the URL
     * Example: your-repository
     * @throws IllegalArgumentException if the repository URL is invalid or the name cannot be extracted
     */
    private static String extractRepoName(String repoUrl) {
        LOGGER.info("Extracting repository name from URL: {}", repoUrl);

        String origRepoUrl = repoUrl;

        // Remove ".git" suffix
        if (repoUrl.endsWith(".git")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - ".git".length());
        }

        // Remove the trailing slash
        if (repoUrl.endsWith("/")) {
            repoUrl = repoUrl.substring(0, repoUrl.length() - 1);
        }

        // Extract the repository name after the last slash
        int lastSlashIndex = repoUrl.lastIndexOf('/');
        if (lastSlashIndex != -1) {
            return repoUrl.substring(lastSlashIndex + 1);
        }

        throw new IllegalArgumentException("Invalid repository URL " + origRepoUrl);
    }

    /**
     * The entry point of the application when trying to read and clone repositories from a CSV file.
     *
     * @param args command-line arguments, expected order:
     *             <ol>
     *                 <li>path to the repositories CSV file</li>
     *                 <li>directory for cloning repositories</li>
     *                 <li>optional LLM argument:
     *                     <ul>
     *                         <li>"gpt-4" to run the {@link GptModel#GPT_4} model</li>
     *                         <li>"mock" to run the mock response defined in predefined_responses.txt</li>
     *                         <li>"dryrun" to run {@link Ashe#run} without a model, skipping the error correction process</li>
     *                         <li>if this argument is omitted, a default model will be used ({@link GptModel#GPT_4})</li>
     *                     </ul>
     *                 </li>
     *             </ol>
     */
    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3) {
            String errorMessage = String.format(
                    "Invalid number of arguments: expected 2 or 3 arguments, but received %d. " +
                            "Required: 1) CSV file absolute path, 2) Directory for cloned repositories. " +
                            "Optional: 3) Model name (LLM). Provided arguments: %s.",
                    args.length, Arrays.toString(args));

            LOGGER.error(errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        String csvFilePath = args[0];
        String repoDir = args[1];

        // If no model is provided, use the default model.
        if (args.length == 2) {
            readAndProcessRepositoriesCsv(csvFilePath, repoDir, Ashe.MODEL);
            return;
        }

        Ashe.MODEL = args[2];
        ModelValidator.validateModel(Ashe.MODEL);
        readAndProcessRepositoriesCsv(csvFilePath, repoDir, Ashe.MODEL);
    }
}
