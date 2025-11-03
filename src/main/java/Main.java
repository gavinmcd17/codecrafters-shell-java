import java.io.File;
import java.io.IOException;
import java.util.*;

public class Main {
    private static final Set<String> VALID_COMMANDS =
            new HashSet<>(Arrays.asList("exit", "echo", "type", "pwd", "cd"));

    private static LinkedList<String> tokens = new LinkedList<>();

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
     * Exit the program with either code 0 or 1
     */
    private static void exit() {
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
     * Echo a user inputted string
     */
    private static void echo() {
        if (tokens.size() <= 1) {
            System.out.println();
            return;
        }

        System.out.println(String.join(" ", tokens.subList(1, tokens.size())));
    }

    /**
     * Give the type of command or the path of program
     */
    private static void type() {
        if (tokens.size() <= 1) {
            System.out.println("Usage: type <command>");
            return;
        }

        String command = tokens.get(1).toLowerCase(); // .toLowerCase(Locale.ROOT)

        if (VALID_COMMANDS.contains(command)) {
            System.out.println(command + " is a shell builtin");
            return;
        } else {
            String path = System.getenv("PATH");
            String [] paths = path.split(File.pathSeparator);

            for (String s : paths) {
                File file = new File(s, command);

                if (file.exists() && file.canExecute()) {
                    System.out.println(command + " is " + file.getAbsolutePath());
                    return;
                }
            }
        }

        System.out.println(command + ": not found");
    }

    private static void pwd() {
        System.out.println(System.getProperty("user.dir"));
    }

    private static void cd() {
        if (tokens.size() <= 1) {
            System.out.println("Usage: cd <path>");
            return;
        }

        String targetPath = tokens.get(1);
        File dir = new File(targetPath);

        if (!dir.isAbsolute()) {
            System.out.println("Not supported");
            return;
        }

        if (!dir.exists() || !dir.isDirectory()) {
            System.out.printf("cd: %s: No such file or directory\n", dir);
            return;
        }

        System.setProperty("user.dir", dir.getAbsolutePath());
    }

    private static boolean tryRun(String input) throws IOException {
        String program = tokens.getFirst().toLowerCase();

        String path = System.getenv("PATH");
        String [] paths = path.split(File.pathSeparator);

        for (String s : paths) {
            File file = new File(s, program);

            if (file.exists() && file.canExecute()) {
                Process process = Runtime.getRuntime().exec(input.split(" "));
                process.getInputStream().transferTo(System.out);
                return true;
            }
        }

        return false;
    }

    /**
     * Entry point of the program
     * @param args not used
     * @throws Exception not used
     */
    public static void main(String[] args) throws Exception {
        Scanner sc = new Scanner(System.in);

        // REPL: Read Eval Print Loop
        while (true) {
            System.out.print("$ ");

            if (!sc.hasNextLine()) break;

            String input = sc.nextLine();
            tokens = parseInput(input.trim());

            // do nothing if input is empty
            if (tokens.isEmpty()) {
                continue;
            }

            // command token is the first token of input
            String cmd = tokens.getFirst();

            switch (cmd) {
                case "exit" -> exit();

                case "echo" -> echo();

                case "type" -> type();

                case "pwd" -> pwd();

                case "cd" -> cd();

                default -> {
                    if (!tryRun(input)) System.out.println(input + ": command not found");
                }
            }
        }

        sc.close();
    }
}
