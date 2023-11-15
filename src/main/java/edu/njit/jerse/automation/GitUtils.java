package edu.njit.jerse.automation;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.File;
import java.io.IOException;

/**
 * The {@code GitUtils} class contains static methods for interacting with Git repositories
 * using the JGit library.
 */
public class GitUtils {
    private static final Logger LOGGER = LogManager.getLogger(GitUtils.class);

    /**
     * Clones a Git repository from a specified remote URL into a local directory.
     * The method allows specifying a particular branch to clone. The repository URL can contain
     * '.git' at the end, but it is not required.
     *
     * @param repoUrl   the URL of the remote repository to clone
     * @param branch    the branch of the repository to clone. If null, the default branch is cloned.
     * @param directory the directory where the repository will be cloned into. It should not exist or
     *                  be an empty directory.
     * @throws GitAPIException if any error occurs during the cloning process
     */
    public static void cloneRepository(String repoUrl, String branch, File directory) throws GitAPIException {
        LOGGER.info("Cloning repository: " + repoUrl);
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(directory)
                .setBranch(branch)
                .call();
        LOGGER.info("Clone completed successfully {}", repoUrl);
    }

    /**
     * Fetches updates from the remote repository associated with a local repository path, without
     * merging those changes.
     *
     * @param repoPath the path to the local repository which is set up to track a remote repository
     * @throws IOException     if the local repository path cannot be accessed
     * @throws GitAPIException if any error occurs during the fetch operation
     */
    public static void fetchRepository(File repoPath) throws IOException, GitAPIException {
        LOGGER.info("Fetching changes for repository: " + repoPath);
        try (Git git = Git.open(repoPath)) {
            git.fetch().call();
            LOGGER.info("Fetch completed successfully {}", repoPath);
        }
    }
}
