import java.io.File;
import java.util.*;

public class Main {
    private static String workingDirectory;

    static void main() throws Exception {
        Scanner sc = new Scanner(System.in);
        workingDirectory = System.getProperty("user.dir");

        while (true) {
            System.out.print("$ ");

            String[] userInput = sc.nextLine().split(" ");
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

    private static String cd(String[] arguments) {
        if (arguments.length == 0) {
            return "";
        }

        String userDirectory = arguments[0];
        File newDirectory = new File(userDirectory);

        if (newDirectory.isDirectory()) {
            workingDirectory = userDirectory;
            return "";
        }

        return String.format("%s: No such file or directory\n", newDirectory.getAbsolutePath());
    }

    private static String pwd() {
        return workingDirectory;
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
        Set<String> validCommands = Set.of("exit", "echo", "type", "pwd");

        if (arguments.length > 0) {
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
        } else {
            return "";
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
