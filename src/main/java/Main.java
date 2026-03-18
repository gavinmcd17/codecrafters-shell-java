import java.io.File;
import java.util.*;

public class Main {
    /** Tracks the shell's current working directory independently of the parent process. */
    private static String workingDirectory;

    /** Builtin commands that are resolved by the shell without consulting {@code PATH}. */
    private static final Set<String> validCommands = Set.of("exit", "echo", "type", "pwd");

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

            String[] userInput = parseInput(sc.nextLine());

            if (userInput.length == 0) {
                continue;
            }

            String command = userInput[0];
            String[] arguments = Arrays.copyOfRange(userInput, 1, userInput.length);

            switch (command) {
                case "exit": {
                    System.exit(0);
                    break;
                }

                case "echo": {
                    System.out.print(echo(arguments));
                    break;
                }

                case "type": {
                    System.out.print(type(arguments));
                    break;
                }

                case "pwd": {
                    System.out.println(pwd());
                    break;
                }

                case "cd": {
                    System.out.print(cd(arguments));
                    break;
                }

                default: {
                    boolean programRan = tryRun(command, arguments);

                    if (!programRan) {
                        System.out.printf("%s: command not found\n", command);
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
     * @return parsed command tokens
     */
    private static String[] parseInput(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean inSingleQuotes = false;
        boolean inDoubleQuotes = false;
        boolean seenBackSlash = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            // If following a backslash, ignore all and insert
            if (seenBackSlash) {
                current.append(c);
                seenBackSlash = false;
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

            if (c == '\\') {
                seenBackSlash = true;
                continue;
            }

            if (c == ' ' && !inSingleQuotes && !inDoubleQuotes) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }

                continue;
            }

            current.append(c);
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens.toArray(new String[0]);
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
     * Given a program name and arguments, try to execute the program.
     *
     * @param programName name of a program in PATH
     * @param arguments arguments to pass to the program
     * @return true if the program was run, otherwise false
     */
    private static boolean tryRun(String programName, String[] arguments) throws Exception {
        File program = findExecutable(programName);

        if (program != null) {
            List<String> command = new ArrayList<>();
            command.add(programName); // executable file
            command.addAll(Arrays.asList(arguments)); // user args

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(workingDirectory));
            Process process = pb.start();
            process.getInputStream().transferTo(System.out);
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
        if (arguments.length == 0) {
            return "";
        }

        String toCheck = arguments[0];

        if (validCommands.contains(toCheck)) {
            return String.format("%s is a shell builtin\n", toCheck);
        } else {
            File program = findExecutable(toCheck);

            if (program != null) {
                return String.format("%s is %s\n", toCheck, program.getAbsolutePath());
            }

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
