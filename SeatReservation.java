import java.util.Scanner;
import java.util.InputMismatchException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

public class SeatReservation {
    private static final LocalDate START_DATE = LocalDate.of(2026, 11, 4);
    private static final LocalDate END_DATE = LocalDate.of(2026, 12, 31);

    private static final Map<LocalDate, char[][]> dateSeatPlans = new HashMap<>();

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        while (true) {
            System.out.println("\n================ MOVIE SEAT RESERVATION SYSTEM ================");
            System.out.print("Enter date (YYYY-MM-DD) or type 'exit' to quit: ");
            String dateInput = input.next().trim();

            if (dateInput.equalsIgnoreCase("exit")) {
                break;
            }

            if (dateInput.equalsIgnoreCase("exit")) {
                break;
            }

            LocalDate selectedDate;

            try {
                selectedDate = LocalDate.parse(dateInput);

                // exception handling for invalid date range
                if (selectedDate.isBefore(START_DATE)) {
                    throw new IllegalArgumentException("\n Invalid date. Bookings are only accepted starting from Nov 4, 2026.");
                }

                if (selectedDate.isAfter(END_DATE)) {
                    throw new IllegalArgumentException("\n No seat plan yet available for this date. (Available until Dec 31, 2026)");
                }

            } catch (DateTimeParseException e) {
                System.out.println("\n Invalid date format. Please use YYYY-MM-DD (e.g., 2026-11-04).");
                continue;
            } catch (IllegalArgumentException e) {
                System.out.println(e.getMessage());
                continue;
            }

            char[][] seats = dateSeatPlans.computeIfAbsent(selectedDate, k -> createNewSeatPlan());

            boolean stayOnDate = true;
            while (stayOnDate) {
                manageReservationsForDate(input, selectedDate, seats);

                System.out.print("\nDo you want to check the seat plan / reserve another seat for " + selectedDate + "? (Y/N): ");
                String choice = input.next().trim();

                if (!choice.equalsIgnoreCase("Y")) {
                    stayOnDate = false;
                }
            }
        }

        System.out.println("\nProgram exited successfully!");
        input.close();
    }
    private static char[][] createNewSeatPlan() {
        char[][] seats = new char[10][9];

        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                seats[i][j] = '*';
            }
        }

        // pwd seats
        seats[5][0] = 'P';
        seats[5][5] = 'P';
        seats[5][8] = 'P';

         return seats;
    }

        private static void manageReservationsForDate(Scanner input, LocalDate selectedDate, char[][] seats) {
        while (true) {
            System.out.println("\n================ MOVIE SEAT RESERVATION ================");
            System.out.println("Legend: [*] Available | [X] Reserved | [P] PWD");
            System.out.println("---------------------------------------------------------");
            
            System.out.printf("%-6s %-8s %-8s %-8s || %-8s %-8s %-8s || %-8s %-8s %-8s%n", 
                "", "Col 1", "Col 2", "Col 3", "Col 4", "Col 5", "Col 6", "Col 7", "Col 8", "Col 9");

            for (int i = 0; i < 10; i++) {
                System.out.printf("%-6s ", "Row " + (i + 1));
                
                for (int j = 0; j < 9; j++) {
                    if (j == 3 || j == 6) {
                        System.out.print("|| ");
                    }

                    String seatStatus = String.valueOf(seats[i][j]);
                    System.out.printf("%-8s ", seatStatus);
                }
                System.out.println();

                // divider between upper & lower sa seats
                if (i == 4) {
                    System.out.println("--------------------------------------------------------------------------------------------------------");
                }
            }

            System.out.print("\nEnter row and column number to reserve separated by space (Enter a negative number to exit): ");
            
               try {
                int row = input.nextInt();

                if (row < 0) {
                    break;
                }

                int col = input.nextInt();

                if (col < 0) {
                    break;
                }
                   
                     if (row < 1 || row > 10 || col < 1 || col > 9) {
                    throw new IndexOutOfBoundsException("\n Invalid row or column.");
                }

                if (seats[row - 1][col - 1] == 'X') {
                    throw new IllegalStateException("\n Seat is already reserved.");
                }

                seats[row - 1][col - 1] = 'X';
                System.out.println("\n Seat/PWD space reserved successfully for " + selectedDate + "!");

            } catch (IndexOutOfBoundsException | IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (InputMismatchException e) {
                System.out.println("\n Invalid input type. Please enter numbers.");
                input.next(); 
            }
        }
    }
}
