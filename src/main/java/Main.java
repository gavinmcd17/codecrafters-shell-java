import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String userInput = sc.nextLine();

            String[] command = userInput.split(" ");

            switch (command[0]) {
                case "exit": {
                    System.exit(0);
                    break;
                }

                case "echo": {
                    if (command.length > 1) {
                        for (int i = 1; i < command.length; i++) {
                            System.out.print(command[i] + " ");
                        }

                        System.out.println();
                    }
                    break;
                }

                default: {
                    System.out.printf("%s: command not found\n", userInput);
                }
            }
        }
    }
}
