import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        loop:
        while(true) {
            System.out.print("$ ");
            String input = sc.nextLine().trim();
            switch (input) {
//                case "exit":
//                case "quit":
//                    System.out.println("Goodbye.");
//                    break loop;
                default:
                    System.out.println(input + ": command not found");
            }
        }
    }
}
