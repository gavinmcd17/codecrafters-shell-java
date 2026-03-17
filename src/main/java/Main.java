import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class Main {
    static final List<String> validCommands = new ArrayList<>(List.of("exit", "echo", "type"));

    public static void main(String[] args) throws Exception {
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
                    echo(arguments);
                    break;
                }

                case "type": {
                    if (arguments.length > 0) {
                        String toCheck = arguments[0];

                        if (validCommands.contains(toCheck)) {
                            System.out.printf("%s is a shell builtin\n", toCheck);
                        } else {
                            System.out.printf("%s: not found\n", toCheck);
                        }
                    }

                    break;
                }

                default: {
                    System.out.printf("%s: command not found\n", command);
                }
            }
        }
    }

    private static void echo(String[] arguments) {
        for (int i = 0; i < arguments.length; i++) {
            if (i > 0) System.out.print(" ");
            System.out.print(arguments[i]);
        }
        System.out.println();
        return;
    }
}
