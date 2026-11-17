import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateAccount {
    private static final Path USERS_FILE = Paths.get("users.json");

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("====================================================");
        System.out.println("          WELCOME TO THE CINEMA SYSTEM              ");
        System.out.println("====================================================");

        boolean isAuthenticated = false;

        while (!isAuthenticated) {
            System.out.println("\n1. Sign Up");
            System.out.println("2. Log In");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            String authChoice = scanner.nextLine().trim();

            switch (authChoice) {
                case "1":
                    handleSignUp(scanner);
                    break;

                case "2":
                    isAuthenticated = handleLogin(scanner);
                    break;

                case "3":
                    System.out.println("Exiting system. Goodbye!");
                    scanner.close();
                    return;

                default:
                    System.out.println("Invalid option. Please select 1, 2, or 3.");
            }
        }

        runCinemaSystem(scanner);
        scanner.close();
    }

    private static void handleSignUp(Scanner scanner) {
        System.out.println("\n--- CREATE AN ACCOUNT ---");
        System.out.print("Enter new username: ");
        String username = scanner.nextLine().trim();

        System.out.print("Enter new password: ");
        String password = scanner.nextLine().trim();

        System.out.print("Confirm password: ");
        String confirmPassword = scanner.nextLine().trim();

        if (username.isEmpty() || password.isEmpty()) {
            System.out.println("Username and password cannot be empty.");
            return;
        }

        if (username.contains("\"") || password.contains("\"")) {
            System.out.println("Username and password cannot contain double quotes (\").");
            return;
        }

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match. Sign up cancelled.");
            return;
        }

        Map<String, String> users = loadUsers();
        if (users.containsKey(username)) {
            System.out.println("Username '" + username + "' is already taken. Please choose another.");
            return;
        }

        users.put(username, password);
        saveUsers(users);
        System.out.println("Sign up successful! Account saved to users.json. You can now log in.");
    }

    private static boolean handleLogin(Scanner scanner) {
        System.out.println("\n--- LOG IN ---");
        int attempts = 0;
        final int MAX_ATTEMPTS = 3;

        while (attempts < MAX_ATTEMPTS) {
            System.out.print("Username: ");
            String username = scanner.nextLine().trim();

            System.out.print("Password: ");
            String password = scanner.nextLine().trim();

            Map<String, String> users = loadUsers();
            if (users.containsKey(username) && users.get(username).equals(password)) {
                System.out.println("\nLogin successful! Welcome, " + username + ".");
                return true;
            } else {
                attempts++;
                System.out.println("Invalid credentials. Attempts remaining: " + (MAX_ATTEMPTS - attempts) + "\n");
            }
        }

        System.out.println("Maximum login attempts reached. Returning to start menu.");
        return false;
    }

    private static Map<String, String> loadUsers() {
        Map<String, String> users = new LinkedHashMap<>();

        if (!Files.exists(USERS_FILE)) {
            users.put("admin", "admin123");
            saveUsers(users);
            return users;
        }

        try {
            String json = Files.readString(USERS_FILE);
            Pattern pattern = Pattern.compile("\"username\"\\s*:\\s*\"([^\"]*)\"\\s*,\\s*\"password\"\\s*:\\s*\"([^\"]*)\"");
            Matcher matcher = pattern.matcher(json);

            while (matcher.find()) {
                users.put(matcher.group(1), matcher.group(2));
            }
        } catch (IOException e) {
            System.err.println("Error reading users.json: " + e.getMessage());
        }

        return users;
    }

    private static void saveUsers(Map<String, String> users) {
        StringBuilder sb = new StringBuilder();
        sb.append("[\n");

        int count = 0;
        for (Map.Entry<String, String> entry : users.entrySet()) {
            sb.append("  { \"username\": \"").append(entry.getKey())
              .append("\", \"password\": \"").append(entry.getValue()).append("\" }");
            if (++count < users.size()) {
                sb.append(",");
            }
            sb.append("\n");
        }

        sb.append("]");

        try {
            Files.writeString(USERS_FILE, sb.toString());
        } catch (IOException e) {
            System.err.println("Error saving to users.json: " + e.getMessage());
        }
    }

    /**
     * Connects CinemaManager, SeatReservation, and BillingCalculator after successful login.
     */
    private static void runCinemaSystem(Scanner scanner) {
        CinemaManager cinemaManager = new CinemaManager();

        while (true) {
            System.out.println("\n================ MAIN MENU ================");
            System.out.println("1. View Movie Schedules & Book Ticket");
            System.out.println("2. Launch Seat Reservation System");
            System.out.println("3. Calculate Billing");
            System.out.println("4. Logout / Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1":
                    System.out.println("\n--- Regular Movies ---");
                    for (MovieGroup group : cinemaManager.getRegularGroups()) {
                        System.out.printf("[%d] %s (%s | %s) - PHP %.2f%n",
                                group.getId(), group.getTitle(), group.getDateRange(), group.getTime(), group.getPrice());
                    }
                    System.out.println("\n--- Promo Movies ---");
                    for (MovieGroup group : cinemaManager.getPromoGroups()) {
                        System.out.printf("[%d] %s (%s | %s) - PHP %.2f%n",
                                group.getId(), group.getTitle(), group.getDateRange(), group.getTime(), group.getPrice());
                    }
                    break;

                case "2":
                    SeatReservation.main(new String[]{});
                    break;

                case "3":
                    try {
                        System.out.print("Enter ticket base price: ");
                        double price = Double.parseDouble(scanner.nextLine().trim());

                        System.out.print("Enter quantity: ");
                        int qty = Integer.parseInt(scanner.nextLine().trim());

                        System.out.print("Apply 20% discount? (true/false): ");
                        boolean discount = Boolean.parseBoolean(scanner.nextLine().trim());

                        System.out.print("Payment method (Cash/GCash/Card): ");
                        String method = scanner.nextLine().trim();

                        BillingCalculator billing = new BillingCalculator(price, qty, discount, method);
                        System.out.printf("Subtotal: PHP %.2f | Discount: PHP %.2f | Online Fee: PHP %.2f | Final Total: PHP %.2f (%s)%n",
                                billing.getSubtotal(), billing.getDiscountAmount(), billing.getOnlineFee(),
                                billing.getFinalTotal(), billing.getPaymentMethod());
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid numeric input.");
                    }
                    break;

                case "4":
                    System.out.println("Logging out. Goodbye!");
                    return;

                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }
}