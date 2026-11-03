import java.util.Scanner;
import java.util.InputMismatchException;

public class SeatReservation {
    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

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

        int row, col;

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
                row = input.nextInt();
                col = input.nextInt();

                if (row < 0 || col < 0) {
                    break;
                }

                if (row < 1 || row > 10 || col < 1 || col > 9) {
                    throw new IndexOutOfBoundsException("\n Invalid row or column.");
                }

                if (seats[row - 1][col - 1] == 'X') {
                    throw new IllegalStateException("\n Seat is already reserved.");
                }

                seats[row - 1][col - 1] = 'X';
                System.out.println("\n Seat/PWD space reserved successfully.");

            } catch (IndexOutOfBoundsException | IllegalStateException e) {
                System.out.println(e.getMessage());
            } catch (InputMismatchException e) {
                System.out.println("\n Invalid input type. Please enter numbers.");
                input.next(); 
            }
        }

        System.out.println("\nProgram exit!");
        input.close();
    }
}