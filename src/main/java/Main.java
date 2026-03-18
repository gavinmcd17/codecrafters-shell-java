import java.io.File;
import java.util.*;

public class Main {
    static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

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

                default: {
                    tryRun(command, arguments);
                    System.out.printf("%s: command not found\n", command);
                }
            }
        }
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

    private static void tryRun(String programName, String[] arguments) throws Exception {
        File program = findExecutable(programName);

        if (program != null) {
            List<String> command = new ArrayList<>();
            command.add(program.getAbsolutePath()); // executable file
            command.addAll(Arrays.asList(arguments)); // user args

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.inheritIO();
            Process process = pb.start();
            process.waitFor();
        }
    }

    private static String type(String[] arguments) {
        Set<String> validCommands = Set.of("exit", "echo", "type");

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

    private static String echo(String[] arguments) {
        StringBuilder toEcho = new StringBuilder();

        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) toEcho.append(' ');
            toEcho.append(arguments[i]);
        }

        return toEcho.append('\n').toString();
    }
}
