import java.util.Scanner;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.LocalDate;
import java.util.Locale;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        CinemaManager manager = new CinemaManager();
        boolean running = true;

        System.out.println("=== SINE-SERVE: Cinema Schedule & Prices ===");
        System.out.println("Testing tip: Today's date is " + LocalDate.now());

        System.out.println("\n=================================================================================================");
        System.out.println("                                AVAILABLE MOVIE LINE-UP & SCHEDULES                              ");
        System.out.println("=================================================================================================");
        System.out.printf("%-4s | %-38s | %-20s | %-8s | %-10s\n", "NO.", "MOVIE TITLE", "DATE RANGE", "TIME", "PRICE");
        System.out.println("-------------------------------------------------------------------------------------------------");
        for (var group : manager.getRegularGroups()) {
            System.out.printf("[%2d]  | %-38s | %-20s | %-8s | PHP %-6.2f\n", 
                group.getId(),
                group.getTitle(), 
                group.getDateRange(), 
                group.getTime(), 
                group.getPrice());
        }

        System.out.println("\n=================================================================================================");
        System.out.println("                         CHRISTMAS HOLIDAY PROMO (ALL MOVIES: PHP 102.00)                        ");
        System.out.println("=================================================================================================");
        System.out.printf("%-4s | %-38s | %-20s | %-8s | %-10s\n", "NO.", "MOVIE TITLE", "DATE RANGE", "TIME", "PRICE");
        System.out.println("-------------------------------------------------------------------------------------------------");
        for (var group : manager.getPromoGroups()) {
            System.out.printf("[%2d]  | %-38s | %-20s | %-8s | PHP %-6.2f\n", 
                group.getId(),
                group.getTitle(), 
                group.getDateRange(), 
                group.getTime(), 
                group.getPrice());
        }
        System.out.println("=================================================================================================");

        DateTimeFormatter flexibleFormatter = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(DateTimeFormatter.ofPattern("[yyyy-MM-dd][MMMM d, yyyy][MMM d, yyyy]"))
            .toFormatter(Locale.ENGLISH);

        while (running) {
            System.out.print("\nEnter date (e.g., 2026-11-05 or November 5, 2026) or type 'exit': ");
            String inputDate = scanner.nextLine();

            if (inputDate.equalsIgnoreCase("exit")) {
                running = false;
                break;
            }

            try {
                LocalDate targetDate = LocalDate.parse(inputDate.trim(), flexibleFormatter);

                if (targetDate.getYear() < 2026) {
                    throw new InvalidDateException("Invalid date: Years below 2026 are not allowed.");
                }
                if (targetDate.getYear() > 2026) {
                    throw new ScheduleNotFoundException("No movie schedule available yet for " + targetDate.getYear() + "!");
                }

                LocalDate minDate = LocalDate.of(2026, 11, 1);
                LocalDate maxDate = LocalDate.of(2026, 12, 31);
                if (targetDate.isBefore(minDate) || targetDate.isAfter(maxDate)) {
                    throw new ScheduleNotFoundException("No movie schedules available for this date (" + targetDate + "). Schedules are only available from November to December 2026.");
                }

                System.out.print("Enter movie number (e.g., 1, 2, 10): ");
                int movieNum = Integer.parseInt(scanner.nextLine().trim());

                var ticket = manager.getMovieTicket(inputDate, movieNum);

                System.out.print("Enter number of tickets: ");
                int quantity = Integer.parseInt(scanner.nextLine().trim());
                if (quantity <= 0) {
                    System.out.println("[Error] Number of tickets must be at least 1.");
                    continue;
                }

                System.out.print("Is any ticket holder a PWD or Senior Citizen? (yes/no): ");
                String discountInput = scanner.nextLine().trim();
                boolean hasDiscount = discountInput.equalsIgnoreCase("yes") || discountInput.equalsIgnoreCase("y");

                // --- PAYMENT METHOD SELECTION ---
                System.out.println("\nSelect Payment Method:");
                System.out.println("[1] GCash");
                System.out.println("[2] Maya");
                System.out.println("[3] Bank Account");
                System.out.println("[4] Over-the-Counter / Cash");
                System.out.print("Enter choice (1-4): ");
                String paymentChoice = scanner.nextLine().trim();

                String paymentMethodName = "";
                switch (paymentChoice) {
                    case "1":
                        paymentMethodName = "GCash";
                        break;
                    case "2":
                        paymentMethodName = "Maya";
                        break;
                    case "3":
                        paymentMethodName = "Bank Account";
                        break;
                    case "4":
                        paymentMethodName = "Cash / Over-the-Counter";
                        break;
                    default:
                        paymentMethodName = "Other / Unspecified";
                        break;
                }

                BillingCalculator billing = new BillingCalculator(ticket.getPrice(), quantity, hasDiscount, paymentMethodName);

                System.out.println("\n=================================================");
                System.out.println("                 BOOKING RECEIPT                 ");
                System.out.println("=================================================");
                System.out.println(" MOVIE       : " + ticket.getTitle());
                System.out.println(" DATE        : " + ticket.getShowDate());
                System.out.println(" TIME        : " + ticket.getShowTime());
                System.out.println(" PRICE/TICKET: PHP " + String.format("%.2f", ticket.getPrice()));
                System.out.println(" QUANTITY    : " + quantity);
                System.out.println(" ------------------------------------------------");
                System.out.println(" Subtotal    : PHP " + String.format("%.2f", billing.getSubtotal()));
                if (hasDiscount) {
                    System.out.println(" PWD/Senior  : -PHP " + String.format("%.2f", billing.getDiscountAmount()) + " (20% OFF)");
                }
                System.out.println(" Online Fee  : PHP " + String.format("%.2f", billing.getOnlineFee()));
                System.out.println(" PAYMENT METH: " + billing.getPaymentMethod());
                System.out.println(" =================================================");
                System.out.println(" TOTAL DUE   : PHP " + String.format("%.2f", billing.getFinalTotal()));
                System.out.println("=================================================");
                System.out.println("                    REMINDERS                    ");
                System.out.println("=================================================");
                System.out.println(" 1. Please save, screenshot, or print this");
                System.out.println("    receipt/e-ticket for verification.           ");
                System.out.println(" 2. Arrive at least 20-30 minutes before showtime");
                System.out.println("    for smooth admission.                        ");
                System.out.println(" 3. This is an online e-ticket. No need to fall  ");
                System.out.println("    in line or claim physical tickets at counter.");
                if (hasDiscount) {
                    System.out.println(" 4. Bring a valid PWD or Senior Citizen ID upon ");
                    System.out.println("    entrance for verification.                   ");
                }
                System.out.println(" 5. Tickets are non-refundable once confirmed.");
                System.out.println("=================================================");

                System.out.print("Do you want to confirm this reservation? (yes/no): ");
                String confirm = scanner.nextLine().trim();

                if (confirm.equalsIgnoreCase("yes") || confirm.equalsIgnoreCase("y")) {
                    System.out.println("\n[Success!] Reservation confirmed. Enjoy your movie!");
                    System.out.println(ticket);
                } else {
                    System.out.println("\n[Cancelled] Reservation was cancelled.");
                }

            } catch (NumberFormatException e) {
                System.out.println("[Error] Please enter a valid numeric value.");
            } catch (DateTimeParseException e) {
                System.out.println("[Error] Invalid date format! Please follow YYYY-MM-DD or Month DD, YYYY.");
            } catch (InvalidDateException e) {
                System.out.println("[Invalid Date] " + e.getMessage());
            } catch (ScheduleNotFoundException e) {
                System.out.println("[Notice] " + e.getMessage());
            }

            System.out.print("\nDo you want to continue booking another ticket or exit? (continue/exit): ");
            String choice = scanner.nextLine().trim();
            if (choice.equalsIgnoreCase("exit") || choice.equalsIgnoreCase("e")) {
                running = false;
            }
        }

        System.out.println("\nThank you for using SINE-SERVE!");
        scanner.close();
    }
}
