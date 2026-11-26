import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import javax.imageio.ImageIO;

public class CreateAccount extends JFrame {

    private static final String FILE_NAME = "accounts.txt";
    private static final int MAX_ATTEMPTS = 3;
    private int loginAttempts = 0;

    private final UITheme.AnimatedCardPane cardContainer;

    public CreateAccount() {
        setTitle("SINE-SERVE | Sign In or Create Account");
        setIconImages(UITheme.getAppIcons());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 650);
        setMinimumSize(new Dimension(860, 580));
        setLocationRelativeTo(null);
        getContentPane().setBackground(UITheme.BG_DEEP);

        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.setBackground(UITheme.BG_DEEP);

        root.add(createBrandingHeroPanel());

        JPanel rightWrapper = new JPanel(new GridBagLayout());
        rightWrapper.setBackground(UITheme.BG_DEEP);
        rightWrapper.setBorder(new EmptyBorder(24, 24, 24, 24));

        cardContainer = new UITheme.AnimatedCardPane();
        cardContainer.add(createLoginPanel(), "LOGIN");
        cardContainer.add(createRegisterPanel(), "REGISTER");

        rightWrapper.add(cardContainer);
        root.add(rightWrapper);

        add(root);
    }

    private JPanel createBrandingHeroPanel() {
        JPanel hero = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(46, 22, 96),
                        w, h, new Color(14, 11, 26)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                g2.setColor(new Color(168, 85, 247, 35));
                g2.fillOval(-60, -40, 320, 320);

                g2.setColor(new Color(236, 72, 153, 28));
                g2.fillOval(w - 220, h - 240, 340, 340);

                g2.setColor(UITheme.BORDER_SUBTLE);
                g2.drawLine(w - 1, 0, w - 1, h);

                g2.dispose();
            }
        };
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBorder(new EmptyBorder(54, 48, 54, 48));

        JLabel logoIcon = new JLabel(new UITheme.CinemaLogoIcon(64));
        logoIcon.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel brandTitle = new JLabel("SINE-SERVE");
        brandTitle.setFont(UITheme.fontDisplay(38));
        brandTitle.setForeground(UITheme.TEXT_WHITE);
        brandTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel tagline = new JLabel("BOOK  •  WATCH  •  ENJOY");
        tagline.setFont(UITheme.fontBold(13));
        tagline.setForeground(UITheme.ACCENT_GLOW);
        tagline.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel desc = new JLabel("<html><div style='width:340px; line-height:1.45;'>"
                + "Reserve your favorite seats in seconds, explore upcoming blockbusters, and enjoy exclusive seasonal promos."
                + "</div></html>");
        desc.setFont(UITheme.fontRegular(14));
        desc.setForeground(UITheme.TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);

        hero.add(Box.createVerticalGlue());
        hero.add(logoIcon);
        hero.add(Box.createRigidArea(new Dimension(0, 18)));
        hero.add(brandTitle);
        hero.add(Box.createRigidArea(new Dimension(0, 6)));
        hero.add(tagline);
        hero.add(Box.createRigidArea(new Dimension(0, 18)));
        hero.add(desc);
        hero.add(Box.createRigidArea(new Dimension(0, 28)));
        hero.add(createFeatureBullet("Interactive auditorium seat selection"));
        hero.add(Box.createRigidArea(new Dimension(0, 12)));
        hero.add(createFeatureBullet("PWD & Senior Citizen priority seating & 20% discount"));
        hero.add(Box.createRigidArea(new Dimension(0, 12)));
        hero.add(createFeatureBullet("Instant perforated e-ticket & barcode generation"));
        hero.add(Box.createVerticalGlue());

        return createCinemaShowcasePanel();
    }

    private JPanel createFeatureBullet(String text) {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

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

    // LEFT PANEL: CINEMA MARQUEE & FULL-HEIGHT POSTER FAN SHOWCASE
private JPanel createCinemaShowcasePanel() {
    JPanel leftPanel = new JPanel() {
        private final Image[] samplePosters = loadSamplePosters();

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();

            // Deep Velvet Stage Gradient
            GradientPaint bg = new GradientPaint(
                    0, 0, new Color(38, 18, 82),
                    w, h, new Color(15, 11, 32)
            );
            g2.setPaint(bg);
            g2.fillRect(0, 0, w, h);

            // Warm Cinema Spotlight Cone from Top-Center
            RadialGradientPaint spotlight = new RadialGradientPaint(
                    new Point(w / 2, 110),
                    Math.max(420, (int) (w * 0.85f)),
                    new float[]{0f, 0.55f, 1f},
                    new Color[]{
                            new Color(168, 85, 247, 85),
                            new Color(236, 72, 153, 32),
                            new Color(15, 11, 32, 0)
                    }
            );
            g2.setPaint(spotlight);
            g2.fillRect(0, 0, w, h);

            // Subtle Decorative Marquee Border Frame + Top Light Bulbs
            g2.setColor(new Color(251, 191, 36, 36));
            g2.drawRoundRect(16, 16, w - 33, h - 32, 24, 24);

            g2.setColor(new Color(251, 191, 36, 130));
            for (int x = 32; x < w - 32; x += 26) {
                g2.fillOval(x, 22, 4, 4);
            }

            // Dynamic Sizing so Posters Fill the Center Vertically Without Overlapping
            int centerX = w / 2;
            int pH = Math.max(210, Math.min(285, (int) (h * 0.36)));
            int pW = (int) (pH * 0.67);
            int fanY = Math.max(155, (int) (h * 0.22));
            int spreadX = (int) (pW * 0.86);

            // Glowing Stage Floor Ellipse Under the Posters
            int floorY = fanY + pH + 8;
            RadialGradientPaint floorGlow = new RadialGradientPaint(
                    new Point(centerX, floorY),
                    pW * 1.6f,
                    new float[]{0f, 1f},
                    new Color[]{new Color(168, 85, 247, 70), new Color(15, 11, 32, 0)}
            );
            g2.setPaint(floorGlow);
            g2.fillOval(centerX - (int) (pW * 1.6), floorY - 16, (int) (pW * 3.2), 32);

            // Angled 3-Poster Cinema Fan
            drawTiltedPoster(g2, samplePosters[0], centerX - spreadX, fanY + 18, pW, pH, -12, new Color(124, 58, 237));
            drawTiltedPoster(g2, samplePosters[2], centerX + spreadX, fanY + 18, pW, pH, 12, new Color(236, 72, 153));
            drawTiltedPoster(g2, samplePosters[1], centerX, fanY, pW + 14, pH + 18, 0, UITheme.ACCENT_GOLD);

            // Right-edge subtle border line
            g2.setColor(UITheme.BORDER_SUBTLE);
            g2.drawLine(w - 1, 0, w - 1, h);
            g2.dispose();
        }

        private void drawTiltedPoster(Graphics2D g2, Image img, int cx, int cy, int pw, int ph, double angleDeg, Color rimColor) {
            Graphics2D pg = (Graphics2D) g2.create();
            UITheme.enableAntiAliasing(pg);
            pg.translate(cx, cy + ph / 2.0);
            pg.rotate(Math.toRadians(angleDeg));

            int x = -pw / 2;
            int y = -ph / 2;

            // Deep Drop Shadow
            pg.setColor(new Color(0, 0, 0, 140));
            pg.fillRoundRect(x + 5, y + 10, pw, ph, 18, 18);

            // Poster Clip
            Shape clip = new RoundRectangle2D.Float(x, y, pw, ph, 18, 18);
            pg.setClip(clip);
            if (img != null) {
                pg.drawImage(img, x, y, pw, ph, null);
            } else {
                GradientPaint fallback = new GradientPaint(x, y, new Color(46, 30, 92), x + pw, y + ph, new Color(26, 18, 54));
                pg.setPaint(fallback);
                pg.fillRect(x, y, pw, ph);
                pg.setColor(UITheme.TEXT_MUTED);
                pg.setFont(UITheme.fontBold(11));
                pg.drawString("SINE-SERVE", x + 28, y + ph / 2);
            }
            pg.setClip(null);

            // Glowing Frame Rim
            pg.setColor(new Color(rimColor.getRed(), rimColor.getGreen(), rimColor.getBlue(), 210));
            pg.setStroke(new BasicStroke(1.8f));
            pg.draw(clip);
            pg.dispose();
        }

        private Image[] loadSamplePosters() {
            Image[] imgs = new Image[3];
            try {
                CinemaManager cm = new CinemaManager();
                List<MovieGroup> list = cm.getRegularGroups();
                for (int i = 0; i < Math.min(3, list.size()); i++) {
                    String path = list.get(i).getPosterPath();
                    if (path != null) {
                        File f = new File(path);
                        if (!f.exists()) f = new File(System.getProperty("user.dir"), path);
                        if (f.exists()) imgs[i] = ImageIO.read(f);
                    }
                }
            } catch (Exception ignored) {}
            return imgs;
        }
    };

    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBorder(new EmptyBorder(46, 44, 42, 44));

    // Top Brand Header
    JPanel brandRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
    brandRow.setOpaque(false);
    brandRow.setAlignmentX(Component.CENTER_ALIGNMENT);
    brandRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

    JLabel logoIcon = new JLabel(new UITheme.CinemaLogoIcon(38));
    JLabel brandName = new JLabel("SINE-SERVE");
    brandName.setFont(UITheme.fontDisplay(30));
    brandName.setForeground(UITheme.TEXT_WHITE);

    UITheme.PillLabel liveBadge = new UITheme.PillLabel(
            "BOX OFFICE LIVE",
            new Color(16, 185, 129, 45),
            new Color(110, 231, 183),
            UITheme.PROMO_GREEN
    );
    liveBadge.setFont(UITheme.fontBold(10));

    brandRow.add(logoIcon);
    brandRow.add(brandName);
    brandRow.add(liveBadge);

    JLabel tagline = new JLabel("BOOK   •   WATCH   •   ENJOY");
    tagline.setFont(UITheme.fontBold(13));
    tagline.setForeground(UITheme.ACCENT_GOLD);
    tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

    leftPanel.add(brandRow);
    leftPanel.add(Box.createRigidArea(new Dimension(0, 8)));
    leftPanel.add(tagline);

    // Flexible Glue Pushes the Bottom Card Down so It NEVER Overlaps the Posters
    leftPanel.add(Box.createVerticalGlue());

    // Now Showing Mini Pill Above the Ticket Strip
    UITheme.PillLabel nowShowingPill = new UITheme.PillLabel(
            "NOW PREMIERING IN ALL 4 AUDITORIUMS",
            new Color(124, 58, 237, 45),
            UITheme.ACCENT_GLOW,
            UITheme.BORDER_SUBTLE
    );
    nowShowingPill.setFont(UITheme.fontBold(10));
    nowShowingPill.setAlignmentX(Component.CENTER_ALIGNMENT);

    // Bottom Cinema Ticket-Stub Strip with Vertical Dividers
    UITheme.RoundedPanel ticketStrip = new UITheme.RoundedPanel(18, new Color(24, 18, 48, 235), UITheme.BORDER_SUBTLE, false);
    ticketStrip.setLayout(new GridBagLayout());
    ticketStrip.setBorder(new EmptyBorder(16, 18, 16, 18));
    ticketStrip.setMaximumSize(new Dimension(480, 78));
    ticketStrip.setAlignmentX(Component.CENTER_ALIGNMENT);

    GridBagConstraints gbc = new GridBagConstraints();
    gbc.gridy = 0;
    gbc.weighty = 1.0;
    gbc.fill = GridBagConstraints.BOTH;

    gbc.gridx = 0;
    gbc.weightx = 1.0;
    ticketStrip.add(createCinemaStatCell("4 HALLS", "Now Showing & Promos"), gbc);

    gbc.gridx = 1;
    gbc.weightx = 0;
    ticketStrip.add(createVerticalDivider(), gbc);

    gbc.gridx = 2;
    gbc.weightx = 1.0;
    ticketStrip.add(createCinemaStatCell("INSTANT PASS", "Digital E-Ticket"), gbc);

    gbc.gridx = 3;
    gbc.weightx = 0;
    ticketStrip.add(createVerticalDivider(), gbc);

    gbc.gridx = 4;
    gbc.weightx = 1.0;
    ticketStrip.add(createCinemaStatCell("20% OFF", "PWD / Senior Priority"), gbc);

    JLabel footerNote = new JLabel("Select your seats live and walk straight to the auditorium.");
    footerNote.setFont(UITheme.fontRegular(12));
    footerNote.setForeground(UITheme.TEXT_MUTED);
    footerNote.setAlignmentX(Component.CENTER_ALIGNMENT);

    leftPanel.add(nowShowingPill);
    leftPanel.add(Box.createRigidArea(new Dimension(0, 14)));
    leftPanel.add(ticketStrip);
    leftPanel.add(Box.createRigidArea(new Dimension(0, 14)));
    leftPanel.add(footerNote);

    return leftPanel;
}

private JComponent createVerticalDivider() {
    JPanel div = new JPanel();
    div.setOpaque(true);
    div.setBackground(UITheme.BORDER_SUBTLE);
    div.setPreferredSize(new Dimension(1, 34));
    return div;
}

private JPanel createCinemaStatCell(String top, String bottom) {
    JPanel cell = new JPanel();
    cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
    cell.setOpaque(false);

    JLabel t = new JLabel(top);
    t.setFont(UITheme.fontBold(11));
    t.setForeground(UITheme.ACCENT_GLOW);
    t.setAlignmentX(Component.CENTER_ALIGNMENT);

    JLabel b = new JLabel(bottom);
    b.setFont(UITheme.fontRegular(11));
    b.setForeground(UITheme.TEXT_WHITE);
    b.setAlignmentX(Component.CENTER_ALIGNMENT);

    cell.add(t);
    cell.add(Box.createRigidArea(new Dimension(0, 3)));
    cell.add(b);
    return cell;
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

        JPanel errorRow = new JPanel(new BorderLayout());
        errorRow.setOpaque(false);
        errorRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorRow.setMaximumSize(new Dimension(346, 36));
        errorRow.setPreferredSize(new Dimension(346, 36));

        JLabel errorLabel = new JLabel(" ", SwingConstants.CENTER);
        errorLabel.setFont(UITheme.fontBold(12));
        errorLabel.setForeground(UITheme.ACCENT_PINK);
        errorRow.add(errorLabel, BorderLayout.CENTER);

        DocumentListener clearErrorOnType = new DocumentListener() {
            private void clear() {
                if (!" ".equals(errorLabel.getText())) {
                    errorLabel.setText(" ");
                }
            }
            @Override public void insertUpdate(DocumentEvent e) { clear(); }
            @Override public void removeUpdate(DocumentEvent e) { clear(); }
            @Override public void changedUpdate(DocumentEvent e) { clear(); }
        };
        userField.getField().getDocument().addDocumentListener(clearErrorOnType);
        passField.getField().getDocument().addDocumentListener(clearErrorOnType);

        UITheme.GradientButton loginBtn = new UITheme.GradientButton("Log In", UITheme.ACCENT_VIOLET, UITheme.ACCENT_PINK, 14);
        loginBtn.setMaximumSize(new Dimension(346, 44));
        loginBtn.setPreferredSize(new Dimension(346, 44));
        loginBtn.setFont(UITheme.fontBold(14));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton guestBtn = UITheme.GradientButton.createGhost("Continue as Guest", UITheme.ACCENT_LIGHT, 14);
        guestBtn.setMaximumSize(new Dimension(346, 40));
        guestBtn.setPreferredSize(new Dimension(346, 40));
        guestBtn.setFont(UITheme.fontBold(13));
        guestBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchRow.setMaximumSize(new Dimension(346, 26));

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
            cardContainer.showAnimated("REGISTER", true);
        });

        switchRow.add(noAccLbl);
        switchRow.add(switchToRegBtn);

        Runnable performLogin = () -> {
            String username = userField.getText().trim();
            String password = new String(passField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                errorLabel.setText("Please enter your username and password.");
                return;
            }

            if (!accountExists(username)) {
                errorLabel.setText("Account does not exist. Please create an account.");
                pulseCreateAccountLink(switchToRegBtn);
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
                    errorLabel.setText("Incorrect password! Attempts remaining: " + remaining);
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
        card.add(errorRow);
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

        JLabel subtitle = new JLabel("Join SINE-SERVE to reserve your cinema seats");
        subtitle.setFont(UITheme.fontRegular(13));
        subtitle.setForeground(UITheme.TEXT_MUTED);
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.IconTextField regUserField = new UITheme.IconTextField("Choose a username", UITheme.VectorIcon.Type.USER);
        UITheme.IconPasswordField regPassField = new UITheme.IconPasswordField("Create a password");
        UITheme.IconPasswordField confirmPassField = new UITheme.IconPasswordField("Confirm your password");

        JPanel regErrorRow = new JPanel(new BorderLayout());
        regErrorRow.setOpaque(false);
        regErrorRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        regErrorRow.setMaximumSize(new Dimension(346, 32));
        regErrorRow.setPreferredSize(new Dimension(346, 32));

        JLabel regErrorLabel = new JLabel(" ", SwingConstants.CENTER);
        regErrorLabel.setFont(UITheme.fontBold(12));
        regErrorLabel.setForeground(UITheme.ACCENT_PINK);
        regErrorRow.add(regErrorLabel, BorderLayout.CENTER);

        UITheme.GradientButton registerBtn = new UITheme.GradientButton("Create Account", UITheme.ACCENT_VIOLET, UITheme.ACCENT_PINK, 14);
        registerBtn.setMaximumSize(new Dimension(346, 44));
        registerBtn.setPreferredSize(new Dimension(346, 44));
        registerBtn.setFont(UITheme.fontBold(14));
        registerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel switchRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        switchRow.setOpaque(false);
        switchRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        switchRow.setMaximumSize(new Dimension(346, 26));

        JLabel hasAccLbl = new JLabel("Already have an account?");
        hasAccLbl.setFont(UITheme.fontRegular(12));
        hasAccLbl.setForeground(UITheme.TEXT_MUTED);

        JButton switchToLoginBtn = new JButton("Log In");
        switchToLoginBtn.setFont(UITheme.fontBold(12));
        switchToLoginBtn.setForeground(UITheme.ACCENT_GLOW);
        switchToLoginBtn.setMargin(new Insets(0, 2, 0, 2));
        switchToLoginBtn.setContentAreaFilled(false);
        switchToLoginBtn.setBorderPainted(false);
        switchToLoginBtn.setFocusPainted(false);
        switchToLoginBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        switchToLoginBtn.addActionListener(e -> {
            regErrorLabel.setText(" ");
            cardContainer.showAnimated("LOGIN", false);
        });

        switchRow.add(hasAccLbl);
        switchRow.add(switchToLoginBtn);

        Runnable performRegister = () -> {
            String username = regUserField.getText().trim();
            String password = new String(regPassField.getPassword());
            String confirm = new String(confirmPassField.getPassword());

            if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                regErrorLabel.setText("All fields are required.");
                return;
            }
            if (username.contains(",") || password.contains(",")) {
                regErrorLabel.setText("Commas are not allowed in username or password.");
                return;
            }
            if (username.equalsIgnoreCase("Guest")) {
                regErrorLabel.setText("'Guest' is a reserved system name.");
                return;
            }
            if (password.length() < 4) {
                regErrorLabel.setText("Password must be at least 4 characters.");
                return;
            }
            if (!password.equals(confirm)) {
                regErrorLabel.setText("Passwords do not match.");
                return;
            }
            if (accountExists(username)) {
                regErrorLabel.setText("Username is already taken.");
                return;
            }

            if (saveAccount(username, password)) {
                regErrorLabel.setText(" ");
                regUserField.setText("");
                regPassField.setText("");
                confirmPassField.setText("");
                ThemedDialog.showInfo(this, "Account Created", "Account created successfully! Please sign in.");
                cardContainer.showAnimated("LOGIN", false);
            } else {
                regErrorLabel.setText("Failed to save account. Please try again.");
            }
        };

        registerBtn.addActionListener(e -> performRegister.run());
        confirmPassField.getField().addActionListener(e -> performRegister.run());

        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(subtitle);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(createFieldGroup("Username", regUserField));
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(createFieldGroup("Password", regPassField));
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(createFieldGroup("Confirm Password", confirmPassField));
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(regErrorRow);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(registerBtn);
        card.add(Box.createVerticalGlue());
        card.add(switchRow);

        return card;
    }

    private JPanel createFieldGroup(String labelText, JComponent field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.CENTER_ALIGNMENT);
        group.setMaximumSize(new Dimension(346, 66));

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(UITheme.fontBold(12));
        lbl.setForeground(UITheme.TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        field.setMaximumSize(new Dimension(346, 42));
        field.setPreferredSize(new Dimension(346, 42));

        group.add(lbl);
        group.add(Box.createRigidArea(new Dimension(0, 5)));
        group.add(field);
        return group;
    }

    private boolean accountExists(String username) {
        File file = new File(FILE_NAME);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length >= 1 && parts[0].trim().equals(username.trim())) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private void pulseCreateAccountLink(JButton linkBtn) {
        Color normalColor = UITheme.ACCENT_GLOW;
        Color highlightColor = UITheme.ACCENT_GOLD;
        final int[] ticks = {0};

        Timer pulseTimer = new Timer(180, evt -> {
            ticks[0]++;
            linkBtn.setForeground(ticks[0] % 2 == 1 ? highlightColor : normalColor);
            if (ticks[0] >= 6) {
                linkBtn.setForeground(normalColor);
                ((Timer) evt.getSource()).stop();
            }
        });
        pulseTimer.setInitialDelay(0);
        pulseTimer.start();
    }

    private boolean verifyLogin(String username, String password) {
        File file = new File(FILE_NAME);
        if (!file.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", 2);
                if (parts.length == 2
                        && parts[0].trim().equals(username.trim())
                        && parts[1].equals(password)) {
                    return true;
                }
            }
        } catch (IOException ignored) {
        }
        return false;
    }

    private boolean saveAccount(String username, String password) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(FILE_NAME, true))) {
            writer.write(username.trim() + "," + password);
            writer.newLine();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
    }
}