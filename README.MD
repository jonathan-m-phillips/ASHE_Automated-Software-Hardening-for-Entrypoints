![ASHE Logo Dark](https://i.ibb.co/415pDMQ/ASHE-Logo-Dark-BG.png#gh-dark-mode-only)
![ASHE Logo Light](https://i.ibb.co/YZdDbzX/ASHE-Logo-Light-BG.png#gh-light-mode-only)

# Automated Software Hardening for Entrypoints

ASHE aims to automatically enhance software robustness against unforeseen or harmful inputs. By focusing on entrypoint
methods—those callable from outside the program—ASHE checks for potential undesired behaviors, such as array overflows
or null-pointer dereferences. When ASHE detects such a vulnerability, it auto-synthesizes patches to harden the
software.

ASHE first employs a minimization tool to reduce a program, ensuring it only encompasses a
single method and that method's dependencies. Subsequently, it utilizes a verification tool, like a pluggable
typechecker, to pinpoint code that may be susceptible to unexpected inputs. Concluding this process, a large language
model (LLM) is used to rewrite the code, ensuring its compliance with the verification tool. Through this
procedure, ASHE generates provably-hardened code.

## Overview

ASHE has three main components: a verification minimizer, a set of verification tools, and a patch generator.  
In our prototype, those three tools are:

- **[Specimin](https://github.com/kelloggm/specimin)**: A tool that, given a Java program P and a set of methods M,
  produces a minimized, compilable version of P preserving the signatures of methods, structure of classes, and other
  specifications used in methods within M.
- **[Checker Framework](https://checkerframework.org/)**: A compiler-integrated tool that checks for specific types of
  errors in the Java code.
- **LLM (e.g., [ChatGPT](https://platform.openai.com/docs/introduction))**: Utilized to generate the expected
  appropriate code patches based on the warnings and errors identified by the Checker Framework.

<br/>

---

## Workflow

1. Use Specimin to minimize the targeted Java code in a temporary directory, focusing on methods that require
   verification.
2. Compile and check the minimized code using the Checker Framework.
3. Any errors found during compilation are prompted to the LLM to generate a patch. If no errors are found, skip
   to step 7.
4. The patch from the LLM response is then applied to the minimized code within the temporary directory.
5. Recompile the modified code using the Checker Framework. If additional errors are identified after compilation,
   repeat steps 3-5 until no further errors are found.
6. Replace the original code that was minimized from the absolute path with the modified code in the temporary
   directory.
7. If there were no original errors, or the original code was successfully overwritten, exit the program.

<br/>

---

## Usage

> 🔴 **Important Note:** ASHE is still under active development, so expect frequent changes to its setup and usage.
> If you'd like to use ASHE, we suggest contacting us first.

### Initial Setup

#### Download ASHE

- Download [ASHE](https://github.com/jonathan-m-phillips/ASHE_Automated-Software-Hardening-for-Entrypoints) from
  our GitHub to your local machine.

#### Creating a Configuration File

- **Using `example.properties` as a Template:**
    - Rename the `example.properties` file in the `resources` directory to `config.properties`.
    - > 🔴 **Important Note:** Ensure this file is ignored by git to prevent any sensitive information from being
      committed. The `config.properties` file will contain your OpenAI API key. If any sensitive information is
      committed, immediately revoke your API key and generate a new one.

#### Downloading Specimin

- Navigate to [Specimin](https://github.com/kelloggm/specimin) on GitHub and download the project to your local
  machine.
- Add the absolute path you downloaded Specimin to, to the `config.properties` file in the `resources` directory,
  replacing the `specimin.tool.path` placeholder.

#### Creating the ChatGPT API Key

1. **Create an OpenAI account:**
    - Create an account on the [OpenAI](https://platform.openai.com) website.
2. **Create an OpenAI API key:**
    - Create an [API](https://platform.openai.com/account/api-keys) key for the ChatGPT API under View API Keys.
3. **Add the API key to `config.properties`:**
    - Copy the API key from OpenAI and paste it into the `config.properties` file in the `resources` directory,
      replacing the `openai.api.key` placeholder.

### Compiling ASHE

> 🔴 **Important Note:** This will be changing to a Gradle build.
1. **Navigate to project directory:**
    - Open your terminal and navigate to the project root directory using the `cd` command.
2. **Compile ASHE:**
    - Execute the following command:
      ```bash
      javac -cp ".:../resources:../../../libs/*" edu/njit/jerse/*.java
      ```

### Running ASHE

1. **Execute ASHE:**
    - > 🔴 **Important Note:** This will be changing to a Gradle build.
    - After successful compilation, run ASHE using the following command:
       ```bash
       java -cp ".:../../../libs/*:../resources" edu.njit.jerse.ashe.ASHE "/root/path/to/your/targeted/project" "path/to/targetFile/Example.java" "com.example.Example#foo()" "llm-model"
       ```

**Command Arguments:**

- **--root:** specifies the root directory of the target project
    - Format: absolute path to the project directory
    - Example: `--root /root/path/to/your/targeted/project`
- **--targetFile:** identifies the source file within the project where the target methods are located
    - Format: relative path from the root directory to the source file
    - Example: `--targetFile path/to/targetFile/Example.java`
- **--targetMethod:** specifies the target method that needs to be preserved
    - Format: `fully.qualified.ClassName#methodName(ParameterType1, ParameterType2, ...)`
    - Example: `--targetMethod com.example.Example#foo()`

**Optional Argument:**
- **--model:** large language model to use. Currently, only GPT-4, mock, and dryrun are supported, where mock is a
  test mode that responds with a mock patch and dryrun is a test mode that skips the error correction process.
    - Example: `gpt-4`, `mock`, or `dryrun`
    - Note: if no LLM is specified, the default is `gpt-4`

### Logging

> 🟡 **Note:** All logs are written to the `logs` directory in the project root directory.

<br/>

---

## Automation Process

ASHE includes an automation layer that streamlines the application of its hardening mechanisms on Java files
and repositories. This is accomplished through two primary components:

### AsheAutomation Class

The AsheAutomation class in edu.njit.jerse.automation automates the application of ASHE's minimization and
error correction features on Java files within a specified directory.

#### Key functionalities include:

- Processing All Java Files: Iterates over all Java files in a given Java source directory, applying ASHE's
  mechanisms to each file. The Java source directory is expected to match Maven/Gradle's standards,
  where the source code is located in `src/main/java`.
- Method-Level Precision: Targets public methods in public classes within the Java files for minimization
  and error correction.

### RepositoryAutomationEngine Class

The RepositoryAutomationEngine class in edu.njit.jerse.automation handles the automation of repository
cloning and Java file processing.

#### Its main features are:

- Repository Management: Clones or fetches repositories listed in a provided CSV file.
- Bulk Processing: Applies AsheAutomation to every Java file within the cloned repositories.
- Customizable Repository Data: Accepts a CSV file with repository URLs and branch information.

<br/>

---

## Automation Usage

/absolute/path/to/project/src/main/java/com/example/foo
/absolute/path/to/project

### Running AsheAutomation
   
1. **Execute AsheAutomation:**
    - Run AsheAutomation using the following Gradle command:
      ```bash
      ./gradlew runAsheAutomation -PmodulePath="/path/to/module" -ProotProjectPath="/path/to/root/project" -Pllm="llm-model"
      ```

**Command Arguments:**

- **--directoryPath:** specifies the absolute path to the directory containing Java files to be processed
    - Format: absolute path to the directory
    - Example: `/absolute/path/to/project/src/main/java/com/example/foo`
- **--projectRootPath:** indicates the absolute root path of the project
    - Format: absolute path to the project's root directory
    - Example: `/absolute/path/to/project`

**Optional Argument:**
- **--model:** large language model to use. Currently, only GPT-4, mock, and dryrun are supported, where mock is a
  test mode that responds with a mock patch and dryrun is a test mode that skips the error correction process.
    - Example: `gpt-4`, `mock`, or `dryrun`
    - Note: if no LLM is specified, the default is `gpt-4`

### Running RepositoryAutomationEngine

To facilitate the automated processing of multiple repositories, RepositoryAutomationEngine requires a CSV
file containing the details of each repository. Follow these steps to create and format your CSV file correctly:

1. **CSV File Structure:**
    - The CSV file should include a header row followed by rows for each repository.
    - Columns in the CSV file must be formatted as follows:
        - `Repository`: This column contains the URL of the Git repository. The URL is not required to end in .git.
        - `Branch`: This column specifies the branch name in the repository that will be cloned or fetched. If
          this column is left empty, the default branch (usually "main" or "master") will be used.

2. **Formatting Guidelines:**
    - Ensure the header row contains exactly two columns named `Repository` and `Branch`.
    - Example format of the header row: `Repository,Branch`
    - Each subsequent row should contain the repository URL in the first column and the branch name in the
      second column.
3. **Creating the File:**
    - Use any text editor or spreadsheet software to create the CSV file.
    - Save the file with a `.csv` extension.

4. **Example CSV Content:**
    ```csv
   Repository,Branch
   https://github.com/example/repo1.git,master
   https://github.com/example/repo2.git,development
   ```
   > 🔴 Important Note: It is crucial to adhere to the specified format for the CSV file to ensure proper
   > processing by the RepositoryAutomationEngine.

5. **Choose an LLM:**
   - To use ChatGPT-4, pass `gpt-4` through the arguments.
   - More LLMs will be added in the future.

6. **Execute RepositoryAutomationEngine:**
    - Run RepositoryAutomationEngine using the following Gradle command:
      ```bash
      ./gradlew runRepositoryAutomation -PrepositoriesCsvPath="/path/to/repositories.csv" -PcloneDirectory="/path/to/clone/directory" -Pllm="llm-model"
      ```

**Command Arguments:**

- **--csvFilePath:** specifies the path to the CSV file containing repository details
    - Format: absolute path to the CSV file
    - Example: `path/to/repositories.csv`
- **--repoDir:** indicates the directory where the repositories should be cloned
    - Format: absolute path to the cloning directory
    - Example: `/path/to/clone/directory`

**Optional Argument:**
- **--model:** large language model to use. Currently, only GPT-4, mock, and dryrun are supported, where mock is a
test mode that responds with a mock patch and dryrun is a test mode that skips the error correction process.
    - Example: `gpt-4`, `mock`, or `dryrun`
    - Note: if no LLM is specified, the default is `gpt-4`

<br/>

---

## Limitations

- While ASHE utilizes Specimin to minimize the targeted Java code, Specimin is still in its early stages and may not
  fully function for complex projects.
- The system's focus is currently on Java, which limits its applicability to other languages.
- There is potential for the LLM to generate patches that do not fully address the identified errors or cause
  additional errors.
- ASHE is currently limited to only utilizing ChatGPT as the LLM. While ChatGPT has shown promising results, the intent
  is to expand the LLM options to include other models - allowing users to select the model that best fits their needs.
- The user will need to create their own OpenAI API key to utilize ChatGPT, as stated in [Usage](#usage).
- AsheAutomation is currently limited to only processing files in the `src/main/java` directory of a Maven/Gradle
  project. This may cause issues with projects that do not follow this standard.

## Contributing

If you have suggestions, bug reports, or contributions to the project, please open an issue or submit a pull request.

## License

This project is licensed under the MIT License. Refer to the LICENSE file for more details.

<br>