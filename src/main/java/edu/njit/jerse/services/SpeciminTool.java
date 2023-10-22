package edu.njit.jerse.services;

import edu.njit.jerse.utils.Configuration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Note: This class is still under development and does not run properly.
 */
public class SpeciminTool {

    /**
     * Note: This method is still under development and does not run properly.
     */
    public void runSpeciminTool(String outputDirectory, String root, String targetFile, String targetMethod) {
        System.out.println("Running SpeciminTool...");

        Configuration config = Configuration.getInstance();
        String speciminPath = config.getPropertyValue("specimin.tool.path");

        try {
            String myArgs = String.format("--outputDirectory \"%s\" --root \"%s\" --targetFile \"%s\" --targetMethod \"%s\"",
                    outputDirectory, root, targetFile, targetMethod);
            String argsWithOption = "--args=" + myArgs;

            List<String> commands = new ArrayList<>();
            commands.add(speciminPath + "/gradlew");
            commands.add("run");
            commands.add(argsWithOption);
            System.out.println("Executing command:");
            for (String command : commands) {
                System.out.println(command);
            }

            ProcessBuilder builder = new ProcessBuilder(commands);
            builder.redirectErrorStream(true); // This merges the error and output streams
            builder.directory(new File(speciminPath));

            Process process = builder.start();

            // Read the output
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            // Wait for the process to finish and check the exit value
            int exitValue = process.waitFor();
            if (exitValue != 0) {
                System.out.println("There was an error executing the command. Exit value: " + exitValue);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
