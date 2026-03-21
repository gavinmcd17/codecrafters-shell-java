import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

public class Main {
    /** Tracks the shell's current working directory independently of the parent process. */
    private static String workingDirectory;

    /** Builtin commands that are resolved by the shell without consulting {@code PATH}. */
    private static final Set<String> validCommands = Set.of("exit", "echo", "type", "pwd");

    /** Parsed shell command with argv tokens and optional redirection metadata. */
    private record ParsedCommand(
            String command,
            String[] arguments,
            String inputRedirect,
            String outputRedirect,
            boolean appendOutput,
            String errorRedirect
    ) {}

    /**
     * Starts the interactive read-eval-print loop.
     *
     * @throws Exception if reading input or launching a child process fails
     */
    static void main() throws Exception {
        Scanner sc = new Scanner(System.in);
        workingDirectory = System.getProperty("user.dir");

        while (true) {
            System.out.print("$ ");

            String userInput = sc.nextLine();
            if (userInput.isEmpty()) continue;

            ParsedCommand parsedInput = parseInput(userInput);
            String command = parsedInput.command();
            String[] arguments = parsedInput.arguments();
            String inputRedirect = parsedInput.inputRedirect();
            String outputRedirect = parsedInput.outputRedirect();
            boolean appendOutput = parsedInput.appendOutput();
            String errorRedirect = parsedInput.errorRedirect();

            if (isBuiltin(command)) {
                prepareRedirectTarget(outputRedirect, appendOutput, false);
                prepareRedirectTarget(errorRedirect, false, true);
            }

            switch (command) {
                case "exit": {
                    System.exit(0);
                    break;
                }

                case "echo": {
                    writeOutput(echo(arguments), outputRedirect, appendOutput);
                    break;
                }

                case "type": {
                    writeOutput(type(arguments), outputRedirect, appendOutput);
                    break;
                }

                case "pwd": {
                    writeOutput(pwd() + '\n', outputRedirect, appendOutput);
                    break;
                }

                case "cd": {
                    String cdError = cd(arguments);
                    if (!cdError.isEmpty()) {
                        writeError(cdError, errorRedirect);
                    }
                    break;
                }

                default: {
                    boolean programRan = tryRun(command, arguments, inputRedirect, outputRedirect, appendOutput, errorRedirect);

                    if (!programRan) {
                        writeError(String.format("%s: command not found\n", command), errorRedirect);
                    }
                }
            }
        }
    }

    /**
     * Changes the shell's current working directory.
     *
     * @param arguments command arguments where the first item is the target directory
     * @return an error message when the directory cannot be resolved, otherwise an empty string
     */
    private static String cd(String[] arguments) {
        if (arguments.length == 0) {
            return "";
        }

        String target = arguments[0];

        if (target.equals("~")) {
            target = System.getenv("HOME");
        }

        File newDirectory = new File(target);

        if (!newDirectory.isAbsolute()) {
            newDirectory = new File(workingDirectory, target);
        }

        if (newDirectory.isDirectory()) {
            try {
                workingDirectory = newDirectory.getCanonicalPath();
                return "";
            } catch (Exception e) {
                return String.format("cd: %s: No such file or directory\n", target);
            }
        }

        return String.format("cd: %s: No such file or directory\n", target);
    }

    /**
     * Returns the shell's current working directory.
     *
     * @return absolute path of the current working directory
     */
    private static String pwd() {
        return workingDirectory;
    }

    /**
     * Splits a raw command line into shell tokens while preserving quoted segments.
     *
     * @param line raw user input
     * @return parsed command tokens and redirection metadata
     */
    private static ParsedCommand parseInput(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean seenBackslash = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // If inside double quotes and seen backslash, ignore some conditions and insert
            if (seenBackslash && inDoubleQuotes) {
                if (c == '\\' || c == '"') {
                    current.append(c);
                    seenBackslash = false;
                    continue;
                }
            }

            // If following a backslash, ignore all conditions and insert
            if (seenBackslash) {
                current.append(c);
                seenBackslash = false;
                continue;
            }

            // Determine if we're entering or leaving a single quote block
            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            // Determine if we're entering or leaving a double quote block
            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }

            // Determine if the next character should have its rules ignored
            if (c == '\\' && !inSingleQuotes) {
                seenBackslash = true;
                continue;
            }

            if (c == ' ' && !inSingleQuotes && !inDoubleQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                continue;
            }

            if (c == '<' && !inSingleQuotes && !inDoubleQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

                tokens.add("<");
                continue;
            }

            if (c == '>' && !inSingleQuotes && !inDoubleQuotes) {
                boolean isAppend = i + 1 < line.length() && line.charAt(i + 1) == '>';

                if (current.toString().equals("1") || current.toString().equals("2")) {
                    String redirectToken = current + ">" + (isAppend ? ">" : "");
                    current.setLength(0);
                    tokens.add(redirectToken);
                    if (isAppend) {
                        i++;
                    }
                    continue;
                }

                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

                tokens.add(isAppend ? ">>" : ">");
                if (isAppend) {
                    i++;
                }
                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        String inputRedirect = null;
        String outputRedirect = null;
        boolean appendOutput = false;
        String errorRedirect = null;
        List<String> arguments = new ArrayList<>();

        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get(i);

            if (token.equals("<") && i + 1 < tokens.size()) {
                inputRedirect = tokens.get(++i);
                continue;
            }

            if ((token.equals(">") || token.equals("1>") || token.equals(">>") || token.equals("1>>")) && i + 1 < tokens.size()) {
                outputRedirect = tokens.get(++i);
                appendOutput = token.endsWith(">>");
                continue;
            }

            if (token.equals("2>") && i + 1 < tokens.size()) {
                errorRedirect = tokens.get(++i);
                continue;
            }

            arguments.add(token);
        }

        String command = arguments.getFirst();
        String[] argv = arguments.subList(1, arguments.size()).toArray(new String[0]);
        return new ParsedCommand(command, argv, inputRedirect, outputRedirect, appendOutput, errorRedirect);
    }

    /**
     * Given a program name, find the associated executable file in PATH.
     *
     * @param programName name of a program in PATH
     * @return File object if program exists, otherwise null
     */
    private static File findExecutable(String programName) {
        String[] paths = System.getenv("PATH").split(File.pathSeparator);

        for (String path : paths) {
            File program = new File(path, programName);

            if (program.exists() && program.canExecute()) {
                return program;
            }
        }

        return null;
    }

    /**
     * Resolves a shell file path against the shell's current working directory.
     *
     * @param path shell path, absolute or relative
     * @return resolved File
     */
    private static File resolvePath(String path) {
        File resolved = new File(path);

        if (!resolved.isAbsolute()) {
            resolved = new File(workingDirectory, path);
        }

        return resolved;
    }

    /**
     * Returns whether the command is handled by the shell instead of a child process.
     *
     * @param command command name
     * @return true when the shell executes the command directly
     */
    private static boolean isBuiltin(String command) {
        return validCommands.contains(command) || command.equals("cd");
    }

    /**
     * Opens and truncates a redirection target so builtins create the file even if they write nothing.
     *
     * @param redirectPath redirection target, or null when not redirected
     * @param useErrorStream when true, setup failures are written to stderr
     */
    private static void prepareRedirectTarget(String redirectPath, boolean append, boolean useErrorStream) throws Exception {
        if (redirectPath == null) {
            return;
        }

        File outputFile = resolvePath(redirectPath);
        File parentDirectory = outputFile.getAbsoluteFile().getParentFile();

        if (parentDirectory != null && !parentDirectory.isDirectory()) {
            if (useErrorStream) {
                System.err.printf("%s: No such file or directory\n", redirectPath);
            } else {
                System.out.printf("%s: No such file or directory\n", redirectPath);
            }
            return;
        }

        try (FileOutputStream ignored = new FileOutputStream(outputFile, append)) {
            // Opening in truncate mode is the behavior we need.
        }
    }

    /**
     * Writes command output either to the terminal or to a redirected stdout file.
     *
     * @param output command output text
     * @param outputRedirect stdout redirection target, or null when writing to the terminal
     */
    private static void writeOutput(String output, String outputRedirect, boolean append) throws Exception {
        writeStream(output, outputRedirect, append, false);
    }

    /**
     * Writes command errors either to the terminal or to a redirected stderr file.
     *
     * @param error command error text
     * @param errorRedirect stderr redirection target, or null when writing to the terminal
     */
    private static void writeError(String error, String errorRedirect) throws Exception {
        writeStream(error, errorRedirect, false, true);
    }

    /**
     * Writes shell output to stdout/stderr or to a redirected file.
     *
     * @param content text to write
     * @param redirectPath redirection target, or null when writing to the terminal
     * @param useErrorStream when true, writes to stderr instead of stdout
     */
    private static void writeStream(String content, String redirectPath, boolean append, boolean useErrorStream) throws Exception {
        if (content.isEmpty()) {
            return;
        }

        if (redirectPath == null) {
            if (useErrorStream) {
                System.err.print(content);
            } else {
                System.out.print(content);
            }
            return;
        }

        File outputFile = resolvePath(redirectPath);
        File parentDirectory = outputFile.getAbsoluteFile().getParentFile();

        if (parentDirectory != null && !parentDirectory.isDirectory()) {
            System.err.printf("%s: No such file or directory\n", redirectPath);
            return;
        }

        try (FileOutputStream outputStream = new FileOutputStream(outputFile, append)) {
            outputStream.write(content.getBytes());
        }
    }

    /**
     * Given a program name and arguments, try to execute the program.
     *
     * @param programName name of a program in PATH
     * @param arguments arguments to pass to the program
     * @param inputRedirect file path to connect to stdin, or null if not redirected
     * @param outputRedirect file path to connect to stdout, or null if not redirected
     * @param errorRedirect file path to connect to stderr, or null if not redirected
     * @return true if the program was run, otherwise false
     */
    private static boolean tryRun(
            String programName,
            String[] arguments,
            String inputRedirect,
            String outputRedirect,
            boolean appendOutput,
            String errorRedirect
    ) throws Exception {
        File program = findExecutable(programName);

        if (program != null) {
            List<String> command = new ArrayList<>();
            command.add(programName); // executable file
            command.addAll(Arrays.asList(arguments)); // user args

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(workingDirectory));

            if (inputRedirect != null) {
                File inputFile = resolvePath(inputRedirect);

                if (!inputFile.isFile()) {
                    writeError(String.format("%s: No such file or directory\n", inputRedirect), errorRedirect);
                    return true;
                }

                pb.redirectInput(inputFile);
            }

            if (outputRedirect != null) {
                File outputFile = resolvePath(outputRedirect);
                File parentDirectory = outputFile.getAbsoluteFile().getParentFile();
                if (parentDirectory != null && !parentDirectory.isDirectory()) {
                    writeError(String.format("%s: No such file or directory\n", outputRedirect), errorRedirect);
                    return true;
                }

                if (appendOutput) {
                    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(outputFile));
                } else {
                    pb.redirectOutput(outputFile);
                }
            }

            if (errorRedirect != null) {
                File errorFile = resolvePath(errorRedirect);
                File parentDirectory = errorFile.getAbsoluteFile().getParentFile();
                if (parentDirectory != null && !parentDirectory.isDirectory()) {
                    System.err.printf("%s: No such file or directory\n", errorRedirect);
                    return true;
                }

                pb.redirectError(errorFile);
            }

            Process process = pb.start();
            if (outputRedirect == null) {
                process.getInputStream().transferTo(System.out);
            }
            if (errorRedirect == null) {
                process.getErrorStream().transferTo(System.err);
            }
            process.waitFor();
            return true;
        }

        return false;
    }

    /**
     * Given shell arguments, describe the command type of the first argument.
     *
     * @param arguments command arguments provided by the user
     * @return description of the command type, or an empty string
     */
    private static String type(String[] arguments) {
        if (arguments.length == 0) return "";

        String toCheck = arguments[0];

        if (validCommands.contains(toCheck)) {
            return String.format("%s is a shell builtin\n", toCheck);
        } else {
            File program = findExecutable(toCheck);

            if (program != null) return String.format("%s is %s\n", toCheck, program.getAbsolutePath());

            return String.format("%s: not found\n", toCheck);
        }
    }

    /**
     * Given shell arguments, join them into a single output line.
     *
     * @param arguments command arguments provided by the user
     * @return joined arguments followed by a newline
     */
    private static String echo(String[] arguments) {
        StringBuilder toEcho = new StringBuilder();

        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) toEcho.append(' ');
            toEcho.append(arguments[i]);
        }

        return toEcho.append('\n').toString();
    }
}
