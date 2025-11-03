import java.util.*;

public class Main {
    private static final Set<String> VALID_COMMANDS =
            new HashSet<>(Arrays.asList("exit", "quit", "echo", "type"));

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
     * Helper function to handle the exit function; exit the program with code 0 or 1
     *
     * @param tokens a linked list of tokens from user input
     */
    private static void handleExit(LinkedList<String> tokens) {
        int code = 0;

        if (tokens.size() > 1) {
            try {
                code = Integer.parseInt(tokens.get(1));
            } catch (NumberFormatException e) {
                System.out.println("Invalid exit code; using 0");
            }
        }

        System.exit(code);
    }

    /**
     * Helper function to handle the echo function; echo user inputted string
     *
     * @param tokens a linked list of tokens from user input
     */
    private static void handleEcho(LinkedList<String> tokens) {
        if (tokens.size() <= 1) {
            System.out.println();
            return;
        }

        System.out.println(String.join(" ", tokens.subList(1, tokens.size())));
    }

    private static void handleType(LinkedList<String> tokens) {
        if (tokens.size() <= 1) {
            System.out.println("Usage: type <command>");
            return;
        }

        String queried = tokens.get(1).toLowerCase(Locale.ROOT);

        if (VALID_COMMANDS.contains(queried)) {
            System.out.println(queried + " is a shell builtin");
        } else {
            System.out.println(queried + ": not found");
        }
    }

    /**
     * Entry point of the program
     * @param args not used
     * @throws Exception not used
     */
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            if (!sc.hasNextLine()) break;

            String input = sc.nextLine();
            LinkedList<String> tokens = parseInput(input.trim());

            if (tokens.isEmpty()) {
                continue;
            }

            String cmd = tokens.getFirst();

            switch (cmd) {
                case "quit":
                case "exit":
                    handleExit(tokens);
                    break;

                case "echo":
                    handleEcho(tokens);
                    break;

                case "type":
                    handleType(tokens);
                    break;

                default:
                    System.out.println(input + ": command not found");
            }
        }

        sc.close();
    }
}
