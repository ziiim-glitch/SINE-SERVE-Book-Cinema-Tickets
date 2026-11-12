import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            CinemaUI cinema = new CinemaUI();
            cinema.setVisible(true);
        });

    }
}