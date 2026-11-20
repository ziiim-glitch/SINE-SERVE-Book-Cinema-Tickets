import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class UITheme {

    //Core Palette
    public static final Color BG_DEEP = new Color(18, 14, 33);         // #120E21
    public static final Color BG_MID = new Color(28, 21, 52);          // #1C1534
    public static final Color CARD_BG = new Color(30, 24, 56);         // #1E1838
    public static final Color CARD_HOVER = new Color(40, 32, 74);      // #28204A
    public static final Color INPUT_BG = new Color(23, 18, 44);        // #17122C
    public static final Color ACCENT_VIOLET = new Color(124, 58, 237); // #7C3AED
    public static final Color ACCENT_LIGHT = new Color(168, 85, 247);  // #A855F7
    public static final Color ACCENT_GLOW = new Color(192, 132, 252);  // #C084FC
    public static final Color ACCENT_PINK = new Color(236, 72, 153);   // #EC4899
    public static final Color ACCENT_GOLD = new Color(251, 191, 36);   // #FBBF24
    public static final Color PROMO_RED = new Color(225, 29, 72);      // #E11D48
    public static final Color PROMO_GREEN = new Color(16, 185, 129);   // #10B981
    public static final Color PROMO_CARD_BG = new Color(36, 22, 48);
    public static final Color TEXT_WHITE = new Color(248, 250, 252);   // #F8FAFC
    public static final Color TEXT_MUTED = new Color(156, 163, 175);   // #9CA3AF
    public static final Color BORDER_SUBTLE = new Color(64, 52, 108);  // #40346C
    public static final Color DISABLED_BG = new Color(25, 20, 42);

    //Segoe UI Font Hierarchy
    public static Font fontDisplay(int size) {
        return new Font("Segoe UI", Font.BOLD, size);
    }

    public static Font fontBold(int size) {
        return new Font("Segoe UI", Font.BOLD, size);
    }

    public static Font fontRegular(int size) {
        return new Font("Segoe UI", Font.PLAIN, size);
    }

    public static void enableAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }


    // 1. GRADIENT BACKGROUND PANEL
    public static class GradientBackgroundPanel extends JPanel {
        private final Color topColor;
        private final Color bottomColor;

        public GradientBackgroundPanel(Color topColor, Color bottomColor) {
            this.topColor = topColor;
            this.bottomColor = bottomColor;
            setOpaque(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);
            GradientPaint gp = new GradientPaint(0, 0, topColor, getWidth() * 0.4f, getHeight(), bottomColor);
            g2.setPaint(gp);
            g2.fillRect(0, 0, getWidth(), getHeight());

            RadialGradientPaint radial = new RadialGradientPaint(
                    new Point(getWidth() / 2, 60),
                    Math.max(250, getWidth() / 2f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(124, 58, 237, 45), new Color(18, 14, 33, 0)}
            );
            g2.setPaint(radial);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    // 2. ROUNDED PANEL WITH DROP SHADOW & ANIMATED HOVER LIFT / GLOW
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color fillColor;
        private final Color borderColor;
        private Color hoverBorderColor;
        private final boolean shadow;
        private boolean hoverLiftEnabled = false;
        private float hoverProgress = 0f;
        private Timer animTimer;

        public RoundedPanel(int radius, Color fillColor, Color borderColor, boolean shadow) {
            this.radius = radius;
            this.fillColor = fillColor;
            this.borderColor = borderColor;
            this.hoverBorderColor = ACCENT_LIGHT;
            this.shadow = shadow;
            setOpaque(false);
        }

        public void enableHoverEffect(Color glowColor) {
            this.hoverLiftEnabled = true;
            this.hoverBorderColor = glowColor;

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), RoundedPanel.this);
                    if (!contains(p)) {
                        animateHover(false);
                    }
                }
            });
        }

        private void animateHover(boolean entering) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animTimer = new Timer(16, e -> {
                float step = 0.14f;
                if (entering) {
                    hoverProgress = Math.min(1f, hoverProgress + step);
                    if (hoverProgress >= 1f) ((Timer) e.getSource()).stop();
                } else {
                    hoverProgress = Math.max(0f, hoverProgress - step);
                    if (hoverProgress <= 0f) ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int shadowOffset = shadow ? 6 : 0;
            int liftY = hoverLiftEnabled ? Math.round(hoverProgress * 4f) : 0;
            int x = shadow ? 3 : 0;
            int y = (shadow ? 5 : 0) - liftY;
            int w = getWidth() - (shadow ? 6 : 1);
            int h = getHeight() - shadowOffset - 1;

            if (shadow) {
                for (int i = 4; i >= 1; i--) {
                    g2.setColor(new Color(0, 0, 0, 14 * (5 - i)));
                    g2.fillRoundRect(x - i + 2, y + i + 2, w + (i * 2) - 4, h + i, radius + i, radius + i);
                }
            }

            Color currentBg = blendColors(fillColor, CARD_HOVER, hoverProgress * 0.6f);
            g2.setColor(currentBg);
            g2.fillRoundRect(x, y, w, h, radius, radius);

            if (borderColor != null) {
                Color currentBorder = blendColors(borderColor, hoverBorderColor, hoverProgress);
                g2.setStroke(new BasicStroke(hoverProgress > 0.2f ? 1.6f : 1f));
                g2.setColor(currentBorder);
                g2.drawRoundRect(x, y, w, h, radius, radius);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 3. GRADIENT & GHOST ANIMATED BUTTON
    public static class GradientButton extends JButton {
        private Color startColor;
        private Color endColor;
        private final boolean ghostMode;
        private final int radius;
        private float hoverAlpha = 0f;
        private boolean pressed = false;
        private Timer timer;

        public GradientButton(String text, Color startColor, Color endColor, int radius) {
            super(text);
            this.startColor = startColor;
            this.endColor = endColor;
            this.ghostMode = false;
            this.radius = radius;
            initButton();
        }

        public static GradientButton createGhost(String text, Color borderColor, int radius) {
            return new GradientButton(text, borderColor, borderColor, radius, true);
        }

        private GradientButton(String text, Color startColor, Color endColor, int radius, boolean ghost) {
            super(text);
            this.startColor = startColor;
            this.endColor = endColor;
            this.ghostMode = ghost;
            this.radius = radius;
            initButton();
        }

        public void setGradientColors(Color start, Color end) {
            this.startColor = start;
            this.endColor = end;
            repaint();
        }

        private void initButton() {
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setForeground(TEXT_WHITE);
            setFont(fontBold(13));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) animate(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    pressed = false;
                    animate(false);
                }

                @Override
                public void mousePressed(MouseEvent e) {
                    if (isEnabled()) {
                        pressed = true;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    pressed = false;
                    repaint();
                }
            });
        }

        private void animate(boolean enter) {
            if (timer != null && timer.isRunning()) timer.stop();
            timer = new Timer(16, e -> {
                if (enter) {
                    hoverAlpha = Math.min(1f, hoverAlpha + 0.15f);
                    if (hoverAlpha >= 1f) ((Timer) e.getSource()).stop();
                } else {
                    hoverAlpha = Math.max(0f, hoverAlpha - 0.15f);
                    if (hoverAlpha <= 0f) ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            timer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();
            int offset = pressed ? 1 : 0;

            if (ghostMode) {
                g2.setColor(new Color(startColor.getRed(), startColor.getGreen(), startColor.getBlue(), (int) (40 * hoverAlpha)));
                g2.fillRoundRect(0, offset, w, h - offset, radius, radius);
                g2.setStroke(new BasicStroke(1.3f));
                g2.setColor(blendColors(startColor, ACCENT_GLOW, hoverAlpha));
                g2.drawRoundRect(1, 1 + offset, w - 3, h - 3 - offset, radius, radius);
            } else if (!isEnabled()) {
                g2.setColor(DISABLED_BG);
                g2.fillRoundRect(0, 0, w, h, radius, radius);
                g2.setColor(BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            } else {
                Color c1 = blendColors(startColor, startColor.brighter(), hoverAlpha * 0.45f);
                Color c2 = blendColors(endColor, endColor.brighter(), hoverAlpha * 0.45f);
                GradientPaint gp = new GradientPaint(0, 0, c1, w, h, c2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, offset, w, h - offset, radius, radius);

                if (hoverAlpha > 0.05f) {
                    g2.setColor(new Color(255, 255, 255, (int) (50 * hoverAlpha)));
                    g2.drawRoundRect(0, offset, w - 1, h - 1 - offset, radius, radius);
                }
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 4. PILL BADGE LABEL
    public static class PillLabel extends JLabel {
        private final Color bgColor;
        private final Color borderColor;

        public PillLabel(String text, Color bgColor, Color textColor, Color borderColor) {
            super(text, SwingConstants.CENTER);
            this.bgColor = bgColor;
            this.borderColor = borderColor;
            setForeground(textColor);
            setFont(fontBold(11));
            setBorder(new EmptyBorder(3, 10, 3, 10));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 5. DIAGONAL CORNER RIBBON (FOR CHRISTMAS PROMOS)
    public static class DiagonalRibbon extends JComponent {
        private final String text;

        public DiagonalRibbon(String text) {
            this.text = text;
            setPreferredSize(new Dimension(88, 88));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            g2.translate(30, -14);
            g2.rotate(Math.toRadians(45));

            GradientPaint gp = new GradientPaint(0, 0, PROMO_RED, 85, 0, PROMO_GREEN);
            g2.setPaint(gp);
            g2.fillRoundRect(0, 0, 96, 22, 6, 6);

            g2.setColor(new Color(255, 255, 255, 110));
            g2.drawRoundRect(0, 0, 95, 21, 6, 6);

            g2.setColor(TEXT_WHITE);
            g2.setFont(fontBold(10));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (96 - fm.stringWidth(text)) / 2;
            int ty = (22 - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);

            g2.dispose();
        }
    }


    // 6. ROUNDED INPUT FIELD WITH ICON, PLACEHOLDER & FOCUS GLOW
    public static class IconTextField extends JPanel {
        private final JTextField textField;
        private boolean focused = false;

        public IconTextField(String placeholder, VectorIcon.Type iconType) {
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(6, 14, 6, 14));
            setPreferredSize(new Dimension(340, 42));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));

            JLabel iconLbl = new JLabel(new VectorIcon(iconType, 16, TEXT_MUTED));
            add(iconLbl, BorderLayout.WEST);

            textField = new JTextField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (getText().isEmpty() && !isFocusOwner()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        enableAntiAliasing(g2);
                        g2.setColor(new Color(156, 163, 175, 145));
                        g2.setFont(fontRegular(13));
                        FontMetrics fm = g2.getFontMetrics();
                        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(placeholder, 2, y);
                        g2.dispose();
                    }
                }
            };
            textField.setOpaque(false);
            textField.setBorder(null);
            textField.setForeground(TEXT_WHITE);
            textField.setCaretColor(ACCENT_GLOW);
            textField.setFont(fontRegular(13));

            textField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    iconLbl.setIcon(new VectorIcon(iconType, 16, ACCENT_GLOW));
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    iconLbl.setIcon(new VectorIcon(iconType, 16, TEXT_MUTED));
                    repaint();
                }
            });

            add(textField, BorderLayout.CENTER);
        }

        public JTextField getField() { return textField; }
        public String getText() { return textField.getText(); }
        public void setText(String t) { textField.setText(t); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (focused) {
                g2.setColor(new Color(168, 85, 247, 45));
                g2.fillRoundRect(0, 0, w, h, 20, 20);
            }

            g2.setColor(INPUT_BG);
            g2.fillRoundRect(2, 2, w - 4, h - 4, 18, 18);

            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.setColor(focused ? ACCENT_LIGHT : BORDER_SUBTLE);
            g2.drawRoundRect(2, 2, w - 5, h - 5, 18, 18);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 7. ROUNDED PASSWORD FIELD WITH LOCK ICON & SHOW/HIDE TOGGLE
    public static class IconPasswordField extends JPanel {
        private final JPasswordField passwordField;
        private final char defaultEchoChar;
        private boolean visible = false;
        private boolean focused = false;

        public IconPasswordField(String placeholder) {
            setOpaque(false);
            setLayout(new BorderLayout(8, 0));
            setBorder(new EmptyBorder(6, 14, 6, 10));
            setPreferredSize(new Dimension(340, 44));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            JLabel lockLbl = new JLabel(new VectorIcon(VectorIcon.Type.LOCK, 16, TEXT_MUTED));
            add(lockLbl, BorderLayout.WEST);

            passwordField = new JPasswordField() {
                @Override
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (getPassword().length == 0 && !isFocusOwner()) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        enableAntiAliasing(g2);
                        g2.setColor(new Color(156, 163, 175, 145));
                        g2.setFont(fontRegular(13));
                        FontMetrics fm = g2.getFontMetrics();
                        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(placeholder, 2, y);
                        g2.dispose();
                    }
                }
            };
            this.defaultEchoChar = passwordField.getEchoChar();
            passwordField.setOpaque(false);
            passwordField.setBorder(null);
            passwordField.setForeground(TEXT_WHITE);
            passwordField.setCaretColor(ACCENT_GLOW);
            passwordField.setFont(fontRegular(14));

            JButton toggleBtn = new JButton(new VectorIcon(VectorIcon.Type.EYE_CLOSED, 16, TEXT_MUTED));
            toggleBtn.setContentAreaFilled(false);
            toggleBtn.setBorderPainted(false);
            toggleBtn.setFocusPainted(false);
            toggleBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            toggleBtn.setToolTipText("Show / Hide Password");
            toggleBtn.setPreferredSize(new Dimension(28, 28));
            toggleBtn.addActionListener(e -> {
                visible = !visible;
                passwordField.setEchoChar(visible ? (char) 0 : defaultEchoChar);
                toggleBtn.setIcon(new VectorIcon(
                        visible ? VectorIcon.Type.EYE_OPEN : VectorIcon.Type.EYE_CLOSED,
                        16,
                        visible ? ACCENT_GLOW : TEXT_MUTED
                ));
            });

            passwordField.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    lockLbl.setIcon(new VectorIcon(VectorIcon.Type.LOCK, 16, ACCENT_GLOW));
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    lockLbl.setIcon(new VectorIcon(VectorIcon.Type.LOCK, 16, TEXT_MUTED));
                    repaint();
                }
            });

            add(passwordField, BorderLayout.CENTER);
            add(toggleBtn, BorderLayout.EAST);
        }

        public JPasswordField getField() { return passwordField; }
        public char[] getPassword() { return passwordField.getPassword(); }
        public void setText(String t) { passwordField.setText(t); }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (focused) {
                g2.setColor(new Color(168, 85, 247, 45));
                g2.fillRoundRect(0, 0, w, h, 14, 14);
            }

            g2.setColor(INPUT_BG);
            g2.fillRoundRect(2, 2, w - 4, h - 4, 12, 12);

            g2.setStroke(new BasicStroke(focused ? 1.6f : 1f));
            g2.setColor(focused ? ACCENT_LIGHT : BORDER_SUBTLE);
            g2.drawRoundRect(2, 2, w - 5, h - 5, 12, 12);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 8. ROUNDED IMAGE CLIPPER
    public static ImageIcon roundImageIcon(ImageIcon source, int cornerRadius) {
        if (source == null) return null;
        int w = source.getIconWidth();
        int h = source.getIconHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();
        enableAntiAliasing(g2);
        g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));
        g2.drawImage(source.getImage(), 0, 0, null);
        g2.dispose();
        return new ImageIcon(output);
    }

    // 9. VECTOR ICONS (FILM REEL LOGO, USER, LOCK, CALENDAR, CLOCK, SNOWFLAKE)
    public static class CinemaLogoIcon implements Icon {
        private final int size;

        public CinemaLogoIcon(int size) {
            this.size = size;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);
            g2.translate(x, y);

            GradientPaint gp = new GradientPaint(0, 0, ACCENT_VIOLET, size, size, ACCENT_PINK);
            g2.setPaint(gp);
            g2.fill(new Ellipse2D.Double(1, 1, size - 2, size - 2));

            g2.setColor(TEXT_WHITE);
            g2.setStroke(new BasicStroke(1.8f));
            int pad = size / 4;
            g2.draw(new Ellipse2D.Double(pad, pad, size - pad * 2, size - pad * 2));

            int r = Math.max(2, size / 14);
            int cx = size / 2;
            int cy = size / 2;
            int dist = size / 6;
            g2.fillOval(cx - r, cy - dist - r, r * 2, r * 2);
            g2.fillOval(cx - r, cy + dist - r, r * 2, r * 2);
            g2.fillOval(cx - dist - r, cy - r, r * 2, r * 2);
            g2.fillOval(cx + dist - r, cy - r, r * 2, r * 2);

            g2.dispose();
        }
    }

    public static class VectorIcon implements Icon {
        public enum Type { USER, LOCK, EYE_OPEN, EYE_CLOSED, CALENDAR, CLOCK, SEARCH, SNOWFLAKE }

        private final Type type;
        private final int size;
        private final Color color;

        public VectorIcon(Type type, int size, Color color) {
            this.type = type;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);
            g2.translate(x, y);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            switch (type) {
                case USER -> {
                    g2.drawOval(size / 2 - 3, 2, 6, 6);
                    g2.drawArc(2, 9, size - 4, 8, 0, 180);
                }
                case LOCK -> {
                    g2.drawRoundRect(3, 7, size - 6, size - 8, 3, 3);
                    g2.drawArc(5, 2, size - 10, 8, 0, 180);
                }
                case EYE_OPEN -> {
                    g2.drawArc(1, 4, size - 2, size - 8, 0, 180);
                    g2.drawArc(1, 4, size - 2, size - 8, 180, 180);
                    g2.fillOval(size / 2 - 2, size / 2 - 2, 4, 4);
                }
                case EYE_CLOSED -> {
                    g2.drawArc(1, 4, size - 2, size - 8, 0, 180);
                    g2.drawLine(2, size - 3, size - 2, 3);
                }
                case CALENDAR -> {
                    g2.drawRoundRect(2, 3, size - 4, size - 5, 3, 3);
                    g2.drawLine(2, 7, size - 2, 7);
                    g2.drawLine(5, 1, 5, 4);
                    g2.drawLine(size - 5, 1, size - 5, 4);
                }
                case CLOCK -> {
                    g2.drawOval(2, 2, size - 4, size - 4);
                    g2.drawLine(size / 2, size / 2, size / 2, 5);
                    g2.drawLine(size / 2, size / 2, size - 5, size / 2);
                }
                case SEARCH -> {
                    g2.drawOval(2, 2, size - 7, size - 7);
                    g2.drawLine(size - 5, size - 5, size - 2, size - 2);
                }
                case SNOWFLAKE -> {
                    int m = size / 2;
                    g2.drawLine(m, 1, m, size - 1);
                    g2.drawLine(1, m, size - 1, m);
                    g2.drawLine(3, 3, size - 3, size - 3);
                    g2.drawLine(size - 3, 3, 3, size - 3);
                }
            }
            g2.dispose();
        }
    }

    public static Color blendColors(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }
}