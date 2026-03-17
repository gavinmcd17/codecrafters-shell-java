import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");
            String userInput = sc.nextLine();

            switch (userInput) {
                case "exit": {
                    System.exit(0);
                }

                default: {
                    System.out.printf("%s: command not found\n", userInput);
                }
            }
        }
    }
}
