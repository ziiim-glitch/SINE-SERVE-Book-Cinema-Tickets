import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class UITheme {

    // Core Cinema Obsidian & Neon Violet Palette
    public static final Color BG_DEEP = new Color(14, 11, 26);         // #0E0B1A
    public static final Color BG_MID = new Color(24, 18, 44);          // #18122C
    public static final Color CARD_BG = new Color(30, 23, 56);         // #1E1738
    public static final Color CARD_HOVER = new Color(42, 33, 78);      // #2A214E
    public static final Color INPUT_BG = new Color(20, 15, 38);        // #140F26
    public static final Color ACCENT_VIOLET = new Color(124, 58, 237); // #7C3AED
    public static final Color ACCENT_LIGHT = new Color(168, 85, 247);  // #A855F7
    public static final Color ACCENT_GLOW = new Color(196, 145, 255);  // #C491FF
    public static final Color ACCENT_PINK = new Color(236, 72, 153);   // #EC4899
    public static final Color ACCENT_GOLD = new Color(251, 191, 36);   // #FBBF24
    public static final Color PROMO_RED = new Color(225, 29, 72);      // #E11D48
    public static final Color PROMO_GREEN = new Color(16, 185, 129);   // #10B981
    public static final Color PROMO_CARD_BG = new Color(36, 20, 48);
    public static final Color TEXT_WHITE = new Color(248, 250, 252);   // #F8FAFC
    public static final Color TEXT_MUTED = new Color(156, 163, 175);   // #9CA3AF
    public static final Color BORDER_SUBTLE = new Color(68, 55, 115);  // #443773
    public static final Color DISABLED_BG = new Color(23, 18, 39);

    // Typography Hierarchy
    private static final String FONT_FAMILY = pickFontFamily();
    private static final String MONO_FAMILY = pickMonoFamily();

    private static String pickFontFamily() {
        java.util.Set<String> available = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Segoe UI", "SF Pro Display", "Helvetica Neue", "Inter", "Roboto", "Ubuntu"}) {
            if (available.contains(f)) return f;
        }
        return Font.SANS_SERIF;
    }

    private static String pickMonoFamily() {
        java.util.Set<String> available = new java.util.HashSet<>(java.util.Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : new String[]{"Consolas", "JetBrains Mono", "SF Mono", "Courier New"}) {
            if (available.contains(f)) return f;
        }
        return Font.MONOSPACED;
    }

    public static Font fontDisplay(int size) { return new Font(FONT_FAMILY, Font.BOLD, size); }
    public static Font fontBold(int size)    { return new Font(FONT_FAMILY, Font.BOLD, size); }
    public static Font fontRegular(int size) { return new Font(FONT_FAMILY, Font.PLAIN, size); }
    public static Font fontMono(int size)    { return new Font(MONO_FAMILY, Font.BOLD, size); }
public static java.util.List<Image> getAppIcons() {
        java.util.List<Image> icons = new java.util.ArrayList<>();
        for (int size : new int[]{16, 24, 32, 48, 64}) {
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            new CinemaLogoIcon(size).paintIcon(null, g2, 0, 0);
            g2.dispose();
            icons.add(img);
        }
        return icons;
    }
    public static void enableAntiAliasing(Graphics2D g2) {
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
    }

    // 1. SMOOTH ANIMATED CARD TRANSITION PANE (SLIDE + FADE)
    public static class AnimatedCardPane extends JPanel {
        private final CardLayout cardLayout = new CardLayout();
        private BufferedImage snapshot;
        private float progress = 1f;
        private int slideDirection = 1;
        private Timer transitionTimer;

        public AnimatedCardPane() {
            setLayout(cardLayout);
            setOpaque(false);
        }

        public void showAnimated(String name, boolean slideLeft) {
            if (getWidth() <= 0 || getHeight() <= 0) {
                cardLayout.show(this, name);
                return;
            }
            if (transitionTimer != null && transitionTimer.isRunning()) {
                transitionTimer.stop();
            }

            snapshot = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = snapshot.createGraphics();
            paint(g2);
            g2.dispose();

            cardLayout.show(this, name);
            slideDirection = slideLeft ? 1 : -1;
            progress = 0f;

            transitionTimer = new Timer(14, e -> {
                progress = Math.min(1f, progress + 0.085f);
                if (progress >= 1f) {
                    snapshot = null;
                    ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            transitionTimer.start();
        }

        @Override
        public void paint(Graphics g) {
            if (snapshot != null && progress < 1f) {
                Graphics2D g2 = (Graphics2D) g.create();
                enableAntiAliasing(g2);
                float ease = 1f - (float) Math.pow(1f - progress, 3); // Cubic ease-out
                int shift = Math.round(36 * slideDirection * (1f - ease));

                // Outgoing panel fades & slides out
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, 1f - ease * 1.15f)));
                g2.drawImage(snapshot, Math.round(-28 * slideDirection * ease), 0, null);

                // Incoming panel fades & slides in
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, ease));
                g2.translate(shift, 0);
                super.paint(g2);
                g2.dispose();
            } else {
                super.paint(g);
            }
        }
    }

    // 2. CURVED REALISTIC IMAX CINEMA SCREEN WITH AMBIENT LIGHT CONE
    public static class CurvedCinemaScreen extends JComponent {
        public CurvedCinemaScreen() {
        setPreferredSize(new Dimension(680, 52));
        setMaximumSize(new Dimension(780, 52));
        setOpaque(false);
            }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();
            int padX = 50;

            // Ambient projection light beam falling onto seats
            Path2D lightCone = new Path2D.Float();
            lightCone.moveTo(padX + 10, 18);
            lightCone.quadTo(w / 2f, 4, w - padX - 10, 18);
            lightCone.lineTo(w - 10, h);
            lightCone.lineTo(10, h);
            lightCone.closePath();

            GradientPaint beamPaint = new GradientPaint(
                    w / 2f, 10, new Color(168, 85, 247, 55),
                    w / 2f, h, new Color(168, 85, 247, 0)
            );
            g2.setPaint(beamPaint);
            g2.fill(lightCone);

            // Outer glow arc
            Path2D screenArc = new Path2D.Float();
            screenArc.moveTo(padX, 22);
            screenArc.quadTo(w / 2f, 4, w - padX, 22);

            g2.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(168, 85, 247, 65));
            g2.draw(screenArc);

            // Crisp luminous screen bar
            g2.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            GradientPaint barGrad = new GradientPaint(padX, 0, ACCENT_VIOLET, w / 2f, 0, TEXT_WHITE, true);
            g2.setPaint(barGrad);
            g2.draw(screenArc);

            // Screen Label
            g2.setFont(fontBold(15));
            g2.setColor(ACCENT_GLOW);
            String label = "S  C  R  E  E  N";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, (w - fm.stringWidth(label)) / 2, 42);

            g2.dispose();
        }
    }

 // 3. REALISTIC 2D CINEMA ARMCHAIR SEAT BUTTON
    public static class TheaterSeatButton extends JButton {
        public enum SeatState { AVAILABLE, SELECTED, RESERVED, PWD, PWD_LOCKED }

        private SeatState state;
        private float hoverProgress = 0f;
        private Timer hoverTimer;

        public TheaterSeatButton(String label, SeatState initialState) {
            super(label);
            this.state = initialState;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));

            // Lock dimensions so GridBagLayout NEVER crushes the seats
            Dimension seatDim = new Dimension(62, 40);
            setPreferredSize(seatDim);
            setMinimumSize(seatDim);
            setMaximumSize(seatDim);

            setFont(fontBold(12));
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    if (isEnabled()) animateHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    animateHover(false);
                }
            });
        }

        public void setSeatState(SeatState newState) {
            this.state = newState;
            repaint();
        }

        private void animateHover(boolean enter) {
            if (hoverTimer != null && hoverTimer.isRunning()) hoverTimer.stop();
            hoverTimer = new Timer(16, e -> {
                if (enter) {
                    hoverProgress = Math.min(1f, hoverProgress + 0.18f);
                    if (hoverProgress >= 1f) ((Timer) e.getSource()).stop();
                } else {
                    hoverProgress = Math.max(0f, hoverProgress - 0.18f);
                    if (hoverProgress <= 0f) ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            hoverTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();
            int lift = Math.round(hoverProgress * 2f);

            Color fill;
            Color stroke;
            Color textCol = TEXT_WHITE;

            switch (state) {
                case SELECTED -> {
                    fill = new Color(147, 51, 234);
                    stroke = TEXT_WHITE;
                }
                case RESERVED -> {
                    fill = new Color(135, 30, 44);
                    stroke = new Color(244, 63, 94);
                    textCol = new Color(254, 205, 211);
                }
                case PWD -> {
                    fill = new Color(202, 138, 4);
                    stroke = ACCENT_GOLD;
                }
                case PWD_LOCKED -> {
                    fill = new Color(88, 62, 18);
                    stroke = new Color(161, 114, 32);
                    textCol = new Color(214, 198, 165);
                }
                default -> {
                    fill = new Color(62, 52, 122);
                    stroke = new Color(124, 108, 214);
                }
            }

            if (hoverProgress > 0f && isEnabled()) {
                fill = blendColors(fill, Color.WHITE, 0.18f * hoverProgress);
                stroke = ACCENT_GLOW;
            }

            // Selection outer neon glow
            if (state == SeatState.SELECTED) {
                g2.setColor(new Color(192, 132, 252, 85));
                g2.fillRoundRect(1, 1, w - 2, h - 2, 14, 14);
            }

            // Main Backrest Cushion
            int bx = 8, by = 4 - lift, bw = w - 16, bh = h - 12;
            GradientPaint seatGrad = new GradientPaint(bx, by, blendColors(fill, Color.WHITE, 0.12f), bx, by + bh, blendColors(fill, Color.BLACK, 0.12f));
            g2.setPaint(seatGrad);
            g2.fillRoundRect(bx, by, bw, bh, 12, 12);

            g2.setStroke(new BasicStroke(state == SeatState.SELECTED ? 1.8f : 1.2f));
            g2.setColor(stroke);
            g2.drawRoundRect(bx, by, bw, bh, 12, 12);

            // Top Headrest Cushion
            g2.setColor(blendColors(fill, Color.WHITE, 0.22f));
            g2.fillRoundRect(bx + 7, by + 2, bw - 14, 5, 4, 4);

            // Left & Right Armrests
            Color armColor = blendColors(fill, Color.BLACK, 0.22f);
            g2.setColor(armColor);
            g2.fillRoundRect(3, by + 9, 6, bh - 7, 5, 5);
            g2.fillRoundRect(w - 9, by + 9, 6, bh - 7, 5, 5);
            g2.setColor(stroke);
            g2.setStroke(new BasicStroke(0.9f));
            g2.drawRoundRect(3, by + 9, 6, bh - 7, 5, 5);
            g2.drawRoundRect(w - 9, by + 9, 6, bh - 7, 5, 5);

            // Bottom Seat Cushion Base
            g2.setColor(blendColors(fill, Color.BLACK, 0.18f));
            g2.fillRoundRect(bx + 2, by + bh - 4, bw - 4, 6, 5, 5);

            // Crisp Seat Label
            g2.setFont(getFont());
            g2.setColor(textCol);
            FontMetrics fm = g2.getFontMetrics();
            String txt = getText();
            int tx = (w - fm.stringWidth(txt)) / 2;
            int ty = by + (bh + fm.getAscent()) / 2 - 1;
            g2.drawString(txt, tx, ty);

            g2.dispose();
        }
    }

    // 4. PERFORATED CINEMA E-TICKET CARD (DIE-CUT NOTCHES + DASHED TEAR LINE)
    public static class PerforatedTicketCard extends JPanel {
        private final int tearOffsetFromBottom;

        public PerforatedTicketCard(int tearOffsetFromBottom) {
            this.tearOffsetFromBottom = tearOffsetFromBottom;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();
            int r = 22;
            int notchR = 13;
            int tearY = h - tearOffsetFromBottom;

            // Build ticket silhouette with semicircular side cutouts
            Area ticketShape = new Area(new RoundRectangle2D.Float(1, 1, w - 3, h - 3, r, r));
            ticketShape.subtract(new Area(new Ellipse2D.Float(-notchR + 1, tearY - notchR, notchR * 2, notchR * 2)));
            ticketShape.subtract(new Area(new Ellipse2D.Float(w - notchR - 2, tearY - notchR, notchR * 2, notchR * 2)));

            // Subtle ticket card gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(34, 26, 66), 0, h, new Color(24, 18, 48));
            g2.setPaint(gp);
            g2.fill(ticketShape);

            // Top glowing header accent bar inside ticket
            Shape oldClip = g2.getClip();
            g2.setClip(ticketShape);
            GradientPaint topStripe = new GradientPaint(0, 0, ACCENT_VIOLET, w, 0, ACCENT_PINK);
            g2.setPaint(topStripe);
            g2.fillRect(0, 0, w, 6);
            g2.setClip(oldClip);

            // Outer border
            g2.setStroke(new BasicStroke(1.4f));
            g2.setColor(ACCENT_LIGHT);
            g2.draw(ticketShape);

            // Perforated dashed tear line across middle
            float[] dash = {6f, 6f};
            g2.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f));
            g2.setColor(BORDER_SUBTLE);
            g2.drawLine(notchR + 8, tearY, w - notchR - 8, tearY);

            g2.dispose();
            super.paintComponent(g);
        }
    }

    // 5. ROUNDED PANEL WITH DROP SHADOW & ANIMATED HOVER LIFT / GLOW
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

            MouseAdapter hoverTracker = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (getMousePosition(true) == null) {
                        animateHover(false);
                    }
                }
            };

            addMouseListener(hoverTracker);
            addContainerListener(new java.awt.event.ContainerAdapter() {
                @Override
                public void componentAdded(java.awt.event.ContainerEvent e) {
                    attachHoverRecursive(e.getChild(), hoverTracker);
                }
            });
        }

        private void attachHoverRecursive(Component comp, MouseAdapter tracker) {
            comp.addMouseListener(tracker);
            if (comp instanceof Container container) {
                for (Component child : container.getComponents()) {
                    attachHoverRecursive(child, tracker);
                }
            }
        }

private void animateHover(boolean entering) {
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            animTimer = new Timer(16, e -> {
                float step = 0.16f;
                boolean actuallyInside = entering && (getMousePosition(true) != null);
                if (actuallyInside) {
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

    // 6. GRADIENT & GHOST ANIMATED BUTTON
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

    // 7. PILL BADGE LABEL
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

    // 8. DIAGONAL CORNER RIBBON (FOR CHRISTMAS PROMOS)
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

    // 9. ROUNDED INPUT FIELD WITH ICON, PLACEHOLDER & FOCUS GLOW
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

    // 10. ROUNDED PASSWORD FIELD WITH LOCK ICON & SHOW/HIDE TOGGLE
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

    // 11. ROUNDED IMAGE CLIPPER
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

    // 12. VECTOR ICONS
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

    // 13. ROUNDED FLAT BUTTON
    public static class SoftButton extends JButton {
        private final int radius;
        private boolean hover = false;

        public SoftButton(String text, int radius) {
            super(text);
            this.radius = radius;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hover = true; repaint(); } }
                @Override public void mouseExited(MouseEvent e) { hover = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);
            Color base = getBackground();
            if (hover && isEnabled()) base = blendColors(base, Color.WHITE, 0.18f);
            g2.setColor(base);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.setColor(blendColors(base, Color.BLACK, 0.25f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static Color blendColors(Color c1, Color c2, float ratio) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }

        // 14. ANIMATED MODERN TOGGLE SWITCH (REPLACES DEFAULT SWING JCHECKBOX)
    public static class ToggleSwitch extends JToggleButton {
        private float thumbPos = 0f;
        private Timer animTimer;

        public ToggleSwitch() {
            setOpaque(false);
            setBorderPainted(false);
            setContentAreaFilled(false);
            setFocusPainted(false);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(44, 24));
            setMinimumSize(new Dimension(44, 24));
            setMaximumSize(new Dimension(44, 24));

            addItemListener(e -> animateToggle(isSelected()));
        }

        @Override
        public void setSelected(boolean b) {
            super.setSelected(b);
            thumbPos = b ? 1f : 0f;
            repaint();
        }

        private void animateToggle(boolean on) {
            if (animTimer != null && animTimer.isRunning()) animTimer.stop();
            animTimer = new Timer(14, e -> {
                if (on) {
                    thumbPos = Math.min(1f, thumbPos + 0.18f);
                    if (thumbPos >= 1f) ((Timer) e.getSource()).stop();
                } else {
                    thumbPos = Math.max(0f, thumbPos - 0.18f);
                    if (thumbPos <= 0f) ((Timer) e.getSource()).stop();
                }
                repaint();
            });
            animTimer.start();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            // Track background
            Color trackOff = new Color(42, 34, 74);
            Color trackOn = PROMO_GREEN;
            g2.setColor(blendColors(trackOff, trackOn, thumbPos));
            g2.fillRoundRect(0, 0, w, h, h, h);

            g2.setStroke(new BasicStroke(1.2f));
            g2.setColor(blendColors(BORDER_SUBTLE, new Color(110, 231, 183), thumbPos));
            g2.drawRoundRect(0, 0, w - 1, h - 1, h, h);

            // Sliding white thumb
            int pad = 3;
            int knobSize = h - (pad * 2);
            int travel = w - knobSize - (pad * 2);
            int kx = pad + Math.round(travel * thumbPos);

            g2.setColor(TEXT_WHITE);
            g2.fillOval(kx, pad, knobSize, knobSize);

            g2.dispose();
        }
    }

    // 15. SELECTABLE PAYMENT OPTION PILL BUTTON
    public static class PaymentOptionButton extends JButton {
        private boolean selectedOption = false;
        private boolean hovered = false;

        public PaymentOptionButton(String label) {
            super(label);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setFont(fontBold(12));
            setForeground(TEXT_WHITE);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(0, 38));

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
            });
        }

        public void setSelectedOption(boolean selected) {
            this.selectedOption = selected;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            if (selectedOption) {
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_VIOLET, w, h, ACCENT_PINK);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(ACCENT_GLOW);
                g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
            } else {
                g2.setColor(hovered ? CARD_HOVER : INPUT_BG);
                g2.fillRoundRect(0, 0, w, h, 12, 12);
                g2.setStroke(new BasicStroke(1f));
                g2.setColor(hovered ? ACCENT_LIGHT : BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, w - 1, h - 1, 12, 12);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
// 16. REALISTIC ROUNDED POPOVER TOOLTIP (REPLACES PLAIN RECTANGULAR TOOLTIP)
    public static class CinemaPopoverToolTip extends JToolTip {
        public CinemaPopoverToolTip() {
            setOpaque(false);
            setBorder(new EmptyBorder(12, 16, 14, 16));
            setForeground(TEXT_WHITE);
            setFont(fontRegular(13));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            enableAntiAliasing(g2);

            int w = getWidth();
            int h = getHeight();

            // Outer ambient glow / shadow
            g2.setColor(new Color(0, 0, 0, 110));
            g2.fillRoundRect(2, 3, w - 4, h - 4, 18, 18);

            // Rich gradient card body
            GradientPaint bgGrad = new GradientPaint(
                    0, 0, new Color(36, 27, 68),
                    w, h, new Color(20, 15, 40)
            );
            g2.setPaint(bgGrad);
            g2.fillRoundRect(1, 1, w - 3, h - 3, 16, 16);

            // Top accent highlight bar
            Shape oldClip = g2.getClip();
            g2.setClip(new java.awt.geom.RoundRectangle2D.Float(1, 1, w - 3, h - 3, 16, 16));
            GradientPaint topBar = new GradientPaint(0, 0, ACCENT_VIOLET, w, 0, ACCENT_PINK);
            g2.setPaint(topBar);
            g2.fillRect(1, 1, w - 3, 4);
            g2.setClip(oldClip);

            // Crisp neon violet border
            g2.setStroke(new BasicStroke(1.3f));
            g2.setColor(ACCENT_LIGHT);
            g2.drawRoundRect(1, 1, w - 3, h - 3, 16, 16);

            g2.dispose();
            super.paintComponent(g);
        }
    }
// 17. RELIABLE 60FPS IN-PANEL FADE + SLIDE TRANSITION ENGINE
    public static class Motion {
        public static float easeOutCubic(float t) {
            t = Math.max(0f, Math.min(1f, t));
            float inv = 1f - t;
            return 1f - (inv * inv * inv);
        }

        public static JPanel wrapWithEntrance(JComponent content, int slideOffsetY, int durationMs) {
            return new JPanel(new BorderLayout()) {
                private float progress = 0f;
                private boolean started = false;
                {
                    setBackground(new Color(14, 10, 28));
                    setOpaque(true);
                    add(content, BorderLayout.CENTER);

                    addHierarchyListener(e -> {
                        if (!started && isShowing()) {
                            started = true;
                            final long start = System.nanoTime();
                            final long durationNs = durationMs * 1_000_000L;

                            Timer timer = new Timer(14, null);
                            timer.addActionListener(evt -> {
                                float raw = (float) (System.nanoTime() - start) / durationNs;
                                progress = easeOutCubic(raw);
                                repaint();
                                if (raw >= 1f) {
                                    progress = 1f;
                                    repaint();
                                    timer.stop();
                                }
                            });
                            timer.start();
                        }
                    });
                }

                @Override
                public void paint(Graphics g) {
                    if (progress >= 0.999f) {
                        super.paint(g);
                        return;
                    }
                    g.setColor(new Color(14, 10, 28));
                    g.fillRect(0, 0, getWidth(), getHeight());

                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, progress))));
                    int dy = (int) ((1f - progress) * slideOffsetY);
                    g2.translate(0, dy);
                    super.paint(g2);
                    g2.dispose();
                }
            };
        }

        public static void animateWindowIn(Window win, int slideOffsetY, int durationMs) {
            if (!(win instanceof JWindow)) return;
            try { win.setOpacity(0f); } catch (Exception ignored) { return; }
            Point target = win.getLocation();
            win.setLocation(target.x, target.y + slideOffsetY);
            final long start = System.nanoTime();
            final long durationNs = durationMs * 1_000_000L;
            Timer timer = new Timer(14, null);
            timer.addActionListener(e -> {
                if (!win.isDisplayable()) { timer.stop(); return; }
                float raw = (float) (System.nanoTime() - start) / durationNs;
                float eased = easeOutCubic(raw);
                try { win.setOpacity(Math.min(1f, eased)); } catch (Exception ignored) {}
                win.setLocation(target.x, (int) (target.y + slideOffsetY * (1f - eased)));
                if (raw >= 1f) {
                    try { win.setOpacity(1f); } catch (Exception ignored) {}
                    win.setLocation(target.x, target.y);
                    timer.stop();
                }
            });
            timer.start();
        }

        public static void fadeWindowOut(Window win, int durationMs, Runnable onComplete) {
            if (win == null || !win.isDisplayable()) {
                if (onComplete != null) onComplete.run();
                return;
            }
            win.setVisible(false);
            win.dispose();
            if (onComplete != null) onComplete.run();
        }
    }

    // 18. STAGGERED FADE + SLIDE-UP CONTAINER FOR MOVIE CARDS
    public static class CascadeRevealPanel extends JPanel {
        private float progress = 0f;
        private boolean started = false;

        public CascadeRevealPanel(Component child, int delayMs, int durationMs) {
            super(new BorderLayout());
            setOpaque(false);
            add(child, BorderLayout.CENTER);

            addHierarchyListener(e -> {
                if (!started && isShowing()) {
                    started = true;
                    Timer delayTimer = new Timer(Math.max(1, delayMs), startEvt -> {
                        ((Timer) startEvt.getSource()).stop();
                        final long start = System.nanoTime();
                        final long durationNs = durationMs * 1_000_000L;

                        Timer anim = new Timer(14, frameEvt -> {
                            float raw = (float) (System.nanoTime() - start) / durationNs;
                            progress = Motion.easeOutCubic(raw);
                            repaint();
                            if (raw >= 1f) {
                                progress = 1f;
                                repaint();
                                ((Timer) frameEvt.getSource()).stop();
                            }
                        });
                        anim.start();
                    });
                    delayTimer.setRepeats(false);
                    delayTimer.start();
                }
            });
        }

        @Override
        public void paint(Graphics g) {
            if (progress >= 0.999f) {
                super.paint(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, progress))));
            int offsetY = (int) ((1f - progress) * 28);
            g2.translate(0, offsetY);
            super.paint(g2);
            g2.dispose();
        }
    }
}