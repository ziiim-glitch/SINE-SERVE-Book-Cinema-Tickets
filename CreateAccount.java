import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class CreateAccount extends JFrame {
    private static final String FILE_NAME = "accounts.txt";

    private int loginAttempts = 0;
    private static final int MAX_ATTEMPTS = 3;

    private CardLayout cardLayout;
    private JPanel cardContainer;

    public CreateAccount() {
        setTitle("SINE-SERVE | Book • Watch • Enjoy");
        setSize(980, 620);
        setMinimumSize(new Dimension(860, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0x12, 0x0E, 0x21));

        JPanel root = new JPanel(new GridLayout(1, 2));
        root.setBackground(UITheme.BG_DEEP);

        root.add(createBrandShowcasePanel());

        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setBackground(UITheme.BG_DEEP);
        rightWrapper.setBorder(new EmptyBorder(24, 28, 24, 28));

        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(createLoginPanel(), "LOGIN");
        cardContainer.add(createRegisterPanel(), "REGISTER");

        rightWrapper.add(cardContainer);
        root.add(rightWrapper);

        add(root);
    }

    private JPanel createBrandShowcasePanel() {
        JPanel brandPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(46, 22, 92),
                        w, h, new Color(18, 14, 33)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(168, 85, 247, 36));
                g2.fill(new Ellipse2D.Double(-70, -50, 320, 320));

                g2.setColor(new Color(236, 72, 153, 28));
                g2.fill(new Ellipse2D.Double(w - 220, h - 240, 300, 300));

                g2.setColor(UITheme.BORDER_SUBTLE);
                g2.drawLine(w - 1, 0, w - 1, h);

                g2.dispose();
            }
        };
        brandPanel.setBorder(new EmptyBorder(40, 44, 40, 44));

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        JLabel logoIcon = new JLabel(new UITheme.CinemaLogoIcon(76));
        logoIcon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandName = new JLabel("SINE-SERVE");
        brandName.setFont(UITheme.fontDisplay(38));
        brandName.setForeground(UITheme.TEXT_WHITE);
        brandName.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("Book • Watch • Enjoy");
        tagline.setFont(UITheme.fontBold(16));
        tagline.setForeground(UITheme.ACCENT_GLOW);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='width: 310px; line-height: 1.45;'>"
                + "Experience hassle-free cinema seat reservations with real-time seat selection, "
                + "exclusive holiday promos, and instant e-ticket receipts.</div></html>");
        desc.setFont(UITheme.fontRegular(13));
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        content.add(logoIcon);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(brandName);
        content.add(Box.createRigidArea(new Dimension(0, 6)));
        content.add(tagline);
        content.add(Box.createRigidArea(new Dimension(0, 18)));
        content.add(desc);
        content.add(Box.createRigidArea(new Dimension(0, 28)));
        content.add(createFeatureBullet("Interactive 90-Seat Cinema Layout"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(createFeatureBullet("PWD / Senior Priority Seating & Discounts"));
        content.add(Box.createRigidArea(new Dimension(0, 10)));
        content.add(createFeatureBullet("Instant Digital Receipt & Barcode"));

        brandPanel.add(content);
        return brandPanel;
    }

private JPanel createFeatureBullet(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel icon = new JLabel(new Icon() {
            @Override public int getIconWidth() { return 16; }
            @Override public int getIconHeight() { return 16; }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(UITheme.ACCENT_PINK);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + 3, y + 8, x + 7, y + 12);
                g2.drawLine(x + 7, y + 12, x + 13, y + 4);
                g2.dispose();
            }
        });

        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontRegular(13));
        lbl.setForeground(UITheme.TEXT_WHITE);

        row.add(icon);
        row.add(lbl);
        return row;
    }

    private JPanel createLoginPanel() {
        UITheme.RoundedPanel card = new UITheme.RoundedPanel(22, UITheme.CARD_BG, UITheme.BORDER_SUBTLE, false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(30, 32, 28, 32));
        card.setPreferredSize(new Dimension(410, 500));

        JLabel title = new JLabel("Welcome to SINE-SERVE!");
        title.setFont(UITheme.fontDisplay(26));
        title.setForeground(UITheme.TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Sign in to book your favorite movies");
        subtitle.setFont(UITheme.fontRegular(13));
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.IconTextField userField = new UITheme.IconTextField("Enter your username", UITheme.VectorIcon.Type.USER);
        UITheme.IconPasswordField passField = new UITheme.IconPasswordField("Enter your password");

        JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);
        errorLabel.setFont(UITheme.fontBold(12));
        errorLabel.setForeground(UITheme.ACCENT_PINK);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton loginBtn = new UITheme.GradientButton("Log In", UITheme.ACCENT_VIOLET, UITheme.ACCENT_PINK, 14);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setPreferredSize(new Dimension(340, 44));
        loginBtn.setFont(UITheme.fontBold(14));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton guestBtn = UITheme.GradientButton.createGhost("Continue as Guest", UITheme.ACCENT_LIGHT, 14);
        guestBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        guestBtn.setPreferredSize(new Dimension(340, 40));
        guestBtn.setFont(UITheme.fontBold(13));
        guestBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel noAccLbl = new JLabel("Don't have an account?");
        noAccLbl.setFont(UITheme.fontRegular(12));
        noAccLbl.setForeground(UITheme.TEXT_MUTED);

        JButton switchToRegBtn = new JButton("Create Account");
        switchToRegBtn.setFont(UITheme.fontBold(12));
        switchToRegBtn.setForeground(UITheme.ACCENT_GLOW);
        switchToRegBtn.setMargin(new Insets(0, 2, 0, 2));
        switchToRegBtn.setContentAreaFilled(false);
        switchToRegBtn.setBorderPainted(false);
        switchToRegBtn.setFocusPainted(false);
        switchToRegBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchToRegBtn.addActionListener(e -> {
            errorLabel.setText(" ");
            cardLayout.show(cardContainer, "REGISTER");
        });

        switchRow.add(noAccLbl);
        switchRow.add(switchToRegBtn);

        Runnable performLogin = () -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter both username and password.");
                return;
            }

            if (verifyLogin(username, password)) {
                errorLabel.setText(" ");
                ThemedDialog.showInfo(this, "SINE-SERVE", "Login successful! Welcome, " + username + ".");
                dispose();
                SwingUtilities.invokeLater(() -> new CinemaUI(username, false).setVisible(true));
            } else {
                loginAttempts++;
                if (loginAttempts >= MAX_ATTEMPTS) {
                    ThemedDialog.showError(this, "Access Denied", "Maximum login attempts reached. Exiting system.");
                    System.exit(0);
                } else {
                    int remaining = MAX_ATTEMPTS - loginAttempts;
                    errorLabel.setText("Invalid credentials! Attempts remaining: " + remaining);
                }
            }
        };

        loginBtn.addActionListener(e -> performLogin.run());
        passField.getField().addActionListener(e -> performLogin.run());

        guestBtn.addActionListener(e -> {
            dispose();
            SwingUtilities.invokeLater(() -> new CinemaUI("Guest", true).setVisible(true));
        });

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 24)));
        card.add(createFieldGroup("Username", userField));
        card.add(Box.createRigidArea(new Dimension(0, 14)));
        card.add(createFieldGroup("Password", passField));
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(errorLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(loginBtn);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(guestBtn);
        card.add(Box.createVerticalGlue());
        card.add(switchRow);

        return card;
    }

    private JPanel createRegisterPanel() {
        UITheme.RoundedPanel card = new UITheme.RoundedPanel(22, UITheme.CARD_BG, UITheme.BORDER_SUBTLE, false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(26, 32, 24, 32));
        card.setPreferredSize(new Dimension(410, 520));

        JLabel title = new JLabel("Create Account");
        title.setFont(UITheme.fontDisplay(26));
        title.setForeground(UITheme.TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitle = new JLabel("Join SINE-SERVE for instant seat booking");
        subtitle.setFont(UITheme.fontRegular(13));
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.IconTextField newUserField = new UITheme.IconTextField("Choose a username", UITheme.VectorIcon.Type.USER);
        UITheme.IconPasswordField newPassField = new UITheme.IconPasswordField("Create a password");
        UITheme.IconPasswordField confirmPassField = new UITheme.IconPasswordField("Confirm your password");

        JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);
        errorLabel.setFont(UITheme.fontBold(12));
        errorLabel.setForeground(UITheme.ACCENT_PINK);
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton regBtn = new UITheme.GradientButton("Create Account", UITheme.ACCENT_VIOLET, UITheme.ACCENT_PINK, 14);
        regBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        regBtn.setPreferredSize(new Dimension(340, 44));
        regBtn.setFont(UITheme.fontBold(14));
        regBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hasAccLbl = new JLabel("Already have an account?");
        hasAccLbl.setFont(UITheme.fontRegular(12));
        hasAccLbl.setForeground(UITheme.TEXT_MUTED);

        JButton switchToLoginBtn = new JButton("Log In");
        switchToLoginBtn.setFont(UITheme.fontBold(12));
        switchToLoginBtn.setForeground(UITheme.ACCENT_GLOW);
        switchToLoginBtn.setMargin(new Insets(0, 1, 0, 2));
        switchToLoginBtn.setContentAreaFilled(false);
        switchToLoginBtn.setBorderPainted(false);
        switchToLoginBtn.setFocusPainted(false);
        switchToLoginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchToLoginBtn.addActionListener(e -> {
            errorLabel.setText(" ");
            cardLayout.show(cardContainer, "LOGIN");
        });

        switchRow.add(hasAccLbl);
        switchRow.add(switchToLoginBtn);

        regBtn.addActionListener(e -> {
            String username = newUserField.getText().trim();
            String password = new String(newPassField.getPassword());
            String confirm = new String(confirmPassField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Username and password cannot be empty.");
                return;
            }
            if (username.contains(",") || password.contains(",")) {
                errorLabel.setText("Commas (,) are not allowed in credentials.");
                return;
            }
            if (!password.equals(confirm)) {
                errorLabel.setText("Passwords do not match!");
                return;
            }
            if (usernameExists(username)) {
                errorLabel.setText("Username already exists! Choose another.");
                return;
            }

            if (saveAccount(username, password)) {
                errorLabel.setText(" ");
                ThemedDialog.showInfo(this, "Account Created", "Account created! Welcome, " + username + ".");
                dispose();
                SwingUtilities.invokeLater(() -> new CinemaUI(username, false).setVisible(true));
            } else {
                errorLabel.setText("Error saving account data.");
            }
        });

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 18)));
        card.add(createFieldGroup("Username", newUserField));
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(createFieldGroup("Password", newPassField));
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(createFieldGroup("Confirm Password", confirmPassField));
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(errorLabel);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(regBtn);
        card.add(Box.createVerticalGlue());
        card.add(switchRow);

        return card;
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.fontBold(12));
        label.setForeground(UITheme.TEXT_MUTED);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);

        group.add(label);
        group.add(Box.createRigidArea(new Dimension(0, 6)));
        group.add(field);
        return group;
    }

    private boolean verifyLogin(String username, String password) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2 && parts[0].equals(username) && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    private boolean usernameExists(String username) {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return false;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length >= 1 && parts[0].equalsIgnoreCase(username)) {
                    return true;
                }
            }
        } catch (IOException ignored) {}
        return false;
    }

    private boolean saveAccount(String username, String password) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE_NAME, true))) {
            pw.println(username + "," + password);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

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