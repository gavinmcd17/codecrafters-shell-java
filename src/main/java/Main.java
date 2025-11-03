import java.util.Scanner;
import java.util.LinkedList;

public class Main {
    /**
     * Parse shell input into a linked list of tokens
     *
     * @param input user input
     * @return linked list of tokens
     */
    private static LinkedList<String> parseInput(String input) {
        LinkedList<String> tokens = new LinkedList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (Character.isWhitespace(c)) {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (!current.isEmpty()) {
            tokens.add(current.toString());
        }

        return tokens;
    }

    /**
     * Entry point of the program
     * @param args not used
     * @throws Exception not used
     */
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        loop:
        while(true) {
            System.out.print("$ ");

            if (!sc.hasNextLine()) break;

            String input = sc.nextLine();
            LinkedList<String> tokens = parseInput(input.trim());

            if (tokens.isEmpty()) {
                continue;
            }

            String cmd = tokens.get(0);

            switch (cmd) {
                case "exit":
                    int code = 0;
                    if (tokens.size() > 1) {
                        try {
                            code = Integer.parseInt(tokens.get(1));
                        } catch (NumberFormatException e) {
                            System.out.println(input + ": command not found");
//                            System.out.println("Invalid exit code; using 0");
                        }
                    }
                    System.exit(code);
                    break;

                default:
                    System.out.println(input + ": command not found");
            }
        }

        sc.close();
    }
}
