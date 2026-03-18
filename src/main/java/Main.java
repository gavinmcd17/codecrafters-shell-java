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
                    System.out.printf("%s: command not found\n", command);
                }
            }
        }
    }

    private static String type(String[] arguments) {
        Set<String> validCommands = Set.of("exit", "echo", "type");
        String[] paths = System.getenv("PATH").split(File.pathSeparator);

        if (arguments.length > 0) {
            String toCheck = arguments[0];

            if (validCommands.contains(toCheck)) {
                return String.format("%s is a shell builtin\n", toCheck);
            } else {
                for (String path : paths) {
                    File file = new File(path, toCheck);

                    if (file.exists() && file.canExecute()) {
                        return String.format("%s is %s\n", toCheck, file.getAbsolutePath());
                    }
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
            if (i > 0) {
                toEcho.append(' ');
            }

            toEcho.append(arguments[i]);
        }

        return toEcho.append('\n').toString();
    }
}
