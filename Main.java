import java.awt.Color;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public class Main {
    public static void main(String[] args) {
        UIManager.put("OptionPane.background", new Color(0x1E, 0x18, 0x38));
        UIManager.put("OptionPane.messageForeground", new Color(0xF3, 0xF4, 0xF6));
        UIManager.put("OptionPane.foreground", new Color(0xF3, 0xF4, 0xF6));
        UIManager.put("OptionPane.messageFont", new Font("Segoe UI", Font.PLAIN, 15));
        UIManager.put("OptionPane.buttonFont", new Font("Segoe UI", Font.BOLD, 13));
        UIManager.put("Panel.background", new Color(0x1E, 0x18, 0x38));
        UIManager.put("OptionPane.border", BorderFactory.createEmptyBorder(20, 24, 16, 24));
        UIManager.put("Button.background", new Color(0x7C, 0x3A, 0xED));
        UIManager.put("Button.foreground", Color.WHITE);
        UIManager.put("Button.focus", new Color(0, 0, 0, 0));
        UIManager.put("ScrollPane.background", new Color(0x12, 0x0E, 0x21));
        UIManager.put("Viewport.background", new Color(0x12, 0x0E, 0x21));

        SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
    }
}