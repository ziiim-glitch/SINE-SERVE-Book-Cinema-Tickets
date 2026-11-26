import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CinemaUI extends JFrame {

    // Unified SINE-SERVE Luxury Palette (100% synced with UITheme)
    private static final Color BG_DARK = UITheme.BG_DEEP;
    private static final Color PANEL_BG = UITheme.BG_MID;
    private static final Color CARD_BG = UITheme.CARD_BG;
    private static final Color ACCENT_PURPLE = UITheme.ACCENT_VIOLET;
    private static final Color ACCENT_PINK = UITheme.ACCENT_PINK;
    private static final Color ACCENT_LIGHT = UITheme.ACCENT_LIGHT;
    private static final Color ACCENT_GLOW = UITheme.ACCENT_GLOW;
    private static final Color ACCENT_GOLD = UITheme.ACCENT_GOLD;
    private static final Color TRACK_BG = new Color(34, 26, 62);
    private static final Color TEXT_WHITE = UITheme.TEXT_WHITE;
    private static final Color TEXT_MUTED = UITheme.TEXT_MUTED;

    private final CinemaManager cinemaManager;
    private final Map<String, char[][]> seatPlans;
    private static final Map<String, ImageIcon> posterCache = new HashMap<>();

    private String currentUser;
    private final boolean isGuest;

    // Search, Filter, Sort & Availability State
    private String activeFilter = "ALL";
    private String searchQuery = "";
    private String sortMode = "FEATURED"; // FEATURED, PRICE_ASC, SEATS_DESC
    private boolean availableOnly = false;

    private CollapsibleSection featuredSectionWrapper;
    private JPanel regularSectionWrapper;
    private JPanel promoSectionWrapper;
    private JPanel regularGridPanel;
    private JPanel promoGridPanel;

    // Cinematic Hero & Session E-Ticket Vault
    private int heroIndex = 0;
    private Timer heroAutoTimer;
    private Timer heroProgressTimer;
    private float heroProgress = 0f;
    private static final int HERO_INTERVAL_MS = 3200;

    private static class BookedTicketRecord {
        final String refCode;
        final String movieTitle;
        final String showDate;
        final String showTime;
        final String seatsSummary;
        final String totalPaid;
        final Runnable openTicketDialog;

        BookedTicketRecord(String refCode, String movieTitle, String showDate, String showTime, String seatsSummary, String totalPaid, Runnable openTicketDialog) {
            this.refCode = refCode;
            this.movieTitle = movieTitle;
            this.showDate = showDate;
            this.showTime = showTime;
            this.seatsSummary = seatsSummary;
            this.totalPaid = totalPaid;
            this.openTicketDialog = openTicketDialog;
        }
    }

    private final List<BookedTicketRecord> bookedTickets = new ArrayList<>();
    private UITheme.GradientButton myTicketsBtn;

    public CinemaUI(String currentUser, boolean isGuest) {
        if (currentUser == null || currentUser.trim().isEmpty()) {
            throw new IllegalArgumentException("A valid user or Guest session is required.");
        }
        this.currentUser = currentUser;
        this.isGuest = isGuest;
        this.cinemaManager = new CinemaManager();
        this.seatPlans = new HashMap<>();

        setTitle("SINE-SERVE — Premier Cinema Experience");
        setIconImages(UITheme.getAppIcons());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 900);
        setMinimumSize(new Dimension(960, 720));
        setLocationRelativeTo(null);
        setSize(1280, 900);
        setMinimumSize(new Dimension(960, 720));
        setLocationRelativeTo(null);
        setExtendedState(JFrame.MAXIMIZED_BOTH);    
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        verifyPosters();

        List<MovieGroup> allMovies = new ArrayList<>(cinemaManager.getRegularGroups());
        allMovies.addAll(cinemaManager.getPromoGroups());

        // Ambient Velvet Stage Canvas
        JPanel mainContent = new AmbientCinemaCanvas();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG_DARK);
        mainContent.setOpaque(true);
        mainContent.setBorder(new EmptyBorder(0, 24, 30, 24));

        // 1. Sleek Glass Navigation Bar (Choreographed Slide-Down Entrance)
        FadeSlidePanel headerWrap = new FadeSlidePanel(new BorderLayout());
        headerWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        headerWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        headerWrap.add(createHeaderBanner(), BorderLayout.CENTER);
        headerWrap.playEntrance(0, -12, 280);
        mainContent.add(headerWrap);
        mainContent.add(Box.createRigidArea(new Dimension(0, 18)));

        // 2. Animated Sliding Pill Filter, Sort & Search Bar
        FadeSlidePanel toolbarWrap = new FadeSlidePanel(new BorderLayout());
        toolbarWrap.setAlignmentX(Component.LEFT_ALIGNMENT);
        toolbarWrap.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        toolbarWrap.add(createFilterAndSearchBar(), BorderLayout.CENTER);
        toolbarWrap.playEntrance(70, 10, 280);
        mainContent.add(toolbarWrap);
        mainContent.add(Box.createRigidArea(new Dimension(0, 22)));

        // 3. Featured Premiere Spotlight (Dual-Layer Cross-Dissolve + Accordion Collapse)
        featuredSectionWrapper = new CollapsibleSection(createTopBrowsingSection(allMovies), 368);
        featuredSectionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        mainContent.add(featuredSectionWrapper);
        mainContent.add(Box.createRigidArea(new Dimension(0, 24)));

        // 4. Now Showing Section
        regularSectionWrapper = new JPanel();
        regularSectionWrapper.setLayout(new BoxLayout(regularSectionWrapper, BoxLayout.Y_AXIS));
        regularSectionWrapper.setOpaque(false);
        regularSectionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        regularSectionWrapper.add(createSectionDivider());
        regularSectionWrapper.add(Box.createRigidArea(new Dimension(0, 20)));
        regularSectionWrapper.add(createMovieSectionTitle(
                "Now Showing",
                isGuest ? "Click any poster for Quick Preview  •  Sign in to book" : "Select a movie to reserve your seats",
                ACCENT_PURPLE,
                ACCENT_PINK
        ));
        regularSectionWrapper.add(Box.createRigidArea(new Dimension(0, 14)));
        regularGridPanel = createMovieGridSection(cinemaManager.getRegularGroups(), false, true);
        regularSectionWrapper.add(regularGridPanel);
        regularSectionWrapper.add(Box.createRigidArea(new Dimension(0, 28)));
        mainContent.add(regularSectionWrapper);

        // 5. Christmas Holiday Promos Section
        promoSectionWrapper = new JPanel();
        promoSectionWrapper.setLayout(new BoxLayout(promoSectionWrapper, BoxLayout.Y_AXIS));
        promoSectionWrapper.setOpaque(false);
        promoSectionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        promoSectionWrapper.add(createSectionDivider());
        promoSectionWrapper.add(Box.createRigidArea(new Dimension(0, 20)));
        promoSectionWrapper.add(createFestivePromoHeader());
        promoSectionWrapper.add(Box.createRigidArea(new Dimension(0, 14)));
        promoGridPanel = createMovieGridSection(cinemaManager.getPromoGroups(), true, true);
        promoSectionWrapper.add(promoGridPanel);
        mainContent.add(promoSectionWrapper);

        mainContent.add(Box.createVerticalGlue());

        JScrollPane pageScrollPane = new JScrollPane(mainContent);
        pageScrollPane.setBorder(null);
        pageScrollPane.setBackground(BG_DARK);
        pageScrollPane.setOpaque(true);
        pageScrollPane.getViewport().setBackground(BG_DARK);
        pageScrollPane.getViewport().setOpaque(true);
        pageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        pageScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pageScrollPane.getVerticalScrollBar().setUI(new SlimPurpleScrollBarUI());
        pageScrollPane.getVerticalScrollBar().addAdjustmentListener(e -> hideSynopsisPopover());
        add(pageScrollPane, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshFilteredMovies(false);
            }
        });
    }

    @Override
    public void dispose() {
        if (heroAutoTimer != null) heroAutoTimer.stop();
        if (heroProgressTimer != null) heroProgressTimer.stop();
        super.dispose();
    }

    private File findImageFile(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        File file = new File(path);
        if (file.exists()) return file;
        File userDirFile = new File(System.getProperty("user.dir"), path);
        if (userDirFile.exists()) return userDirFile;
        return null;
    }

    private ImageIcon loadPoster(String path, int targetWidth, int targetHeight, int cornerRadius) {
        if (path == null || path.trim().isEmpty()) return null;

        String cacheKey = path + "_" + targetWidth + "x" + targetHeight + "_r" + cornerRadius;
        if (posterCache.containsKey(cacheKey)) {
            return posterCache.get(cacheKey);
        }

        File imgFile = findImageFile(path);
        if (imgFile == null) {
            return null;
        }

        try {
            BufferedImage originalImg = ImageIO.read(imgFile);
            if (originalImg == null) return null;

            int origW = originalImg.getWidth();
            int origH = originalImg.getHeight();

            double scale = Math.max((double) targetWidth / origW, (double) targetHeight / origH);
            int scaledW = Math.max(1, (int) Math.round(origW * scale));
            int scaledH = Math.max(1, (int) Math.round(origH * scale));

            BufferedImage step = originalImg;
            if (step.getType() == 0 || step.getType() == BufferedImage.TYPE_CUSTOM) {
                BufferedImage conv = new BufferedImage(origW, origH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gc = conv.createGraphics();
                gc.drawImage(step, 0, 0, null);
                gc.dispose();
                step = conv;
            }
            int curW = origW, curH = origH;
            while (curW / 2 >= scaledW && curH / 2 >= scaledH) {
                curW = curW / 2;
                curH = curH / 2;
                BufferedImage tmp = new BufferedImage(curW, curH, BufferedImage.TYPE_INT_ARGB);
                Graphics2D gt = tmp.createGraphics();
                gt.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                gt.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                gt.drawImage(step, 0, 0, curW, curH, null);
                gt.dispose();
                step = tmp;
            }

            BufferedImage croppedImg = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = croppedImg.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);

            int x = (targetWidth - scaledW) / 2;
            int y = (targetHeight - scaledH) / 2;
            g2.drawImage(step, x, y, scaledW, scaledH, null);
            g2.dispose();

            ImageIcon icon = UITheme.roundImageIcon(new ImageIcon(croppedImg), cornerRadius);
            posterCache.put(cacheKey, icon);
            return icon;

        } catch (Exception e) {
            return null;
        }
    }

    private void verifyPosters() {
        List<MovieGroup> allMovies = new ArrayList<>(cinemaManager.getRegularGroups());
        allMovies.addAll(cinemaManager.getPromoGroups());
        for (MovieGroup m : allMovies) {
            findImageFile(m.getPosterPath());
        }
    }

    // AMBIENT VELVET CINEMA STAGE BACKGROUND
    private static class AmbientCinemaCanvas extends JPanel implements Scrollable {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(BG_DARK);
            g2.fillRect(0, 0, w, h);

            RadialGradientPaint violetGlow = new RadialGradientPaint(
                    new Point(220, 120),
                    Math.max(520, w / 2f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(124, 58, 237, 42), new Color(18, 14, 33, 0)}
            );
            g2.setPaint(violetGlow);
            g2.fillRect(0, 0, w, Math.min(h, 920));

            RadialGradientPaint roseGlow = new RadialGradientPaint(
                    new Point(Math.max(320, w - 220), 180),
                    Math.max(460, w / 2.3f),
                    new float[]{0f, 1f},
                    new Color[]{new Color(236, 72, 153, 28), new Color(18, 14, 33, 0)}
            );
            g2.setPaint(roseGlow);
            g2.fillRect(0, 0, w, Math.min(h, 920));

            g2.dispose();
        }

        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 60; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override
        public boolean getScrollableTracksViewportHeight() {
            if (getParent() instanceof JViewport) {
                return getParent().getHeight() > getPreferredSize().height;
            }
            return false;
        }
    }

    // Smooth Fade-In + Directional Slide Wrapper
    private static class FadeSlidePanel extends JPanel {
        private float alpha = 1.0f;
        private int offsetX = 0;
        private int offsetY = 0;

        public FadeSlidePanel(LayoutManager layout) {
            super(layout);
            setOpaque(false);
        }

        public void playEntrance(int delayMs, int slidePixelsY, int durationMs) {
            playDirectionalEntrance(delayMs, 0, slidePixelsY, durationMs);
        }

        public void playDirectionalEntrance(int delayMs, int slidePixelsX, int slidePixelsY, int durationMs) {
            this.alpha = 0f;
            this.offsetX = slidePixelsX;
            this.offsetY = slidePixelsY;
            repaint();

            Runnable startAnim = () -> {
                final long start = System.currentTimeMillis();
                Timer timer = new Timer(15, null);
                timer.addActionListener(e -> {
                    float raw = Math.min(1f, (System.currentTimeMillis() - start) / (float) durationMs);
                    float ease = 1f - (float) Math.pow(1f - raw, 3);
                    alpha = ease;
                    offsetX = Math.round(slidePixelsX * (1f - ease));
                    offsetY = Math.round(slidePixelsY * (1f - ease));
                    repaint();
                    if (raw >= 1f) {
                        alpha = 1f;
                        offsetX = 0;
                        offsetY = 0;
                        timer.stop();
                    }
                });
                timer.start();
            };

            if (delayMs > 0) {
                Timer delayTimer = new Timer(delayMs, e -> {
                    ((Timer) e.getSource()).stop();
                    startAnim.run();
                });
                delayTimer.setRepeats(false);
                delayTimer.start();
            } else {
                startAnim.run();
            }
        }

        @Override
        public void paint(Graphics g) {
            if (alpha <= 0.01f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            if (alpha < 0.999f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            }
            if (offsetX != 0 || offsetY != 0) {
                g2.translate(offsetX, offsetY);
            }
            super.paint(g2);
            g2.dispose();
        }
    }

    // Smooth Accordion Collapse Wrapper for Spotlight Section
    private static class CollapsibleSection extends JPanel {
        private final int expandedHeight;
        private int currentHeight;
        private boolean expanded = true;
        private Timer animTimer;

        public CollapsibleSection(JComponent content, int expandedHeight) {
            super(new BorderLayout());
            setOpaque(false);
            this.expandedHeight = expandedHeight;
            this.currentHeight = expandedHeight;
            add(content, BorderLayout.CENTER);
        }

        public boolean isExpanded() {
            return expanded;
        }

        public void setExpandedAnimated(boolean targetExpanded) {
            if (this.expanded == targetExpanded) return;
            this.expanded = targetExpanded;
            if (animTimer != null && animTimer.isRunning()) {
                animTimer.stop();
            }
            if (targetExpanded) {
                setVisible(true);
            }

            final int startH = currentHeight;
            final int endH = targetExpanded ? expandedHeight : 0;
            final long startTime = System.currentTimeMillis();
            final int duration = 220;

            animTimer = new Timer(15, e -> {
                float raw = Math.min(1f, (System.currentTimeMillis() - startTime) / (float) duration);
                float ease = 1f - (float) Math.pow(1f - raw, 3);
                currentHeight = Math.round(startH + (endH - startH) * ease);
                revalidate();
                if (getParent() != null) {
                    getParent().revalidate();
                    getParent().repaint();
                }
                if (raw >= 1f) {
                    currentHeight = endH;
                    animTimer.stop();
                    if (!targetExpanded) {
                        setVisible(false);
                    }
                }
            });
            animTimer.start();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, currentHeight);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, currentHeight);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, currentHeight);
        }
    }

    // 1. SLEEK GLASS TOP NAVIGATION BAR
    private JPanel createHeaderBanner() {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();
                int r = 22;

                Path2D path = new Path2D.Float();
                path.moveTo(0, 0);
                path.lineTo(w, 0);
                path.lineTo(w, h - r);
                path.quadTo(w, h, w - r, h);
                path.lineTo(r, h);
                path.quadTo(0, h, 0, h - r);
                path.closePath();

                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(46, 24, 96, 245),
                        w, h, new Color(26, 19, 52, 245)
                );
                g2.setPaint(gp);
                g2.fill(path);

                GradientPaint hairline = new GradientPaint(0, h, ACCENT_LIGHT, w, h, ACCENT_PINK);
                g2.setPaint(hairline);
                g2.setStroke(new BasicStroke(1.2f));
                g2.draw(path);

                g2.dispose();
            }
        };

        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(14, 24, 14, 24));
        banner.setPreferredSize(new Dimension(0, 80));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        leftBox.setOpaque(false);

        JLabel iconLbl = new JLabel(new UITheme.CinemaLogoIcon(40));

        JPanel brandCol = new JPanel();
        brandCol.setLayout(new BoxLayout(brandCol, BoxLayout.Y_AXIS));
        brandCol.setOpaque(false);

        JPanel titleLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleLine.setOpaque(false);
        titleLine.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel logo = new JLabel("SINE-SERVE");
        logo.setFont(UITheme.fontDisplay(24));
        logo.setForeground(TEXT_WHITE);

        UITheme.PillLabel premiereBadge = new UITheme.PillLabel(
                "MACJ",
                new Color(124, 58, 237, 48),
                ACCENT_GLOW,
                UITheme.BORDER_SUBTLE
        );
        premiereBadge.setFont(UITheme.fontBold(10));

        titleLine.add(logo);
        titleLine.add(Box.createRigidArea(new Dimension(12, 0)));
        titleLine.add(premiereBadge);

        JLabel subTag = new JLabel(isGuest
        ? "Guest Preview Mode  •  Sign in to unlock seat reservations"
        : "Premier Cinema Ticket & Seat Reservation Experience");
        subTag.setFont(UITheme.fontRegular(12));
        subTag.setForeground(TEXT_MUTED);

        brandCol.add(titleLine);
        brandCol.add(Box.createRigidArea(new Dimension(0, 2)));
        brandCol.add(subTag);

        leftBox.add(iconLbl);
        leftBox.add(brandCol);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        rightBox.setOpaque(false);

        if (!isGuest) {
            myTicketsBtn = UITheme.GradientButton.createGhost("My E-Tickets (0)", ACCENT_GOLD, 16);
            myTicketsBtn.setPreferredSize(new Dimension(154, 36));
            myTicketsBtn.setFont(UITheme.fontBold(12));
            myTicketsBtn.setForeground(ACCENT_GOLD);
            myTicketsBtn.addActionListener(e -> showMyTicketsWalletDialog());
            rightBox.add(myTicketsBtn);
        }

        String displayName = isGuest ? "Guest" : currentUser;
        String initial = isGuest ? "G" : displayName.substring(0, 1).toUpperCase();

        JPanel avatarCircle = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, ACCENT_PURPLE, getWidth(), getHeight(), ACCENT_PINK);
                g2.setPaint(gp);
                g2.fill(new Ellipse2D.Double(0, 0, getWidth(), getHeight()));
                g2.setColor(new Color(255, 255, 255, 130));
                g2.draw(new Ellipse2D.Double(0, 0, getWidth() - 1, getHeight() - 1));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCircle.setOpaque(false);
        avatarCircle.setPreferredSize(new Dimension(36, 36));
        JLabel initLbl = new JLabel(initial);
        initLbl.setFont(UITheme.fontBold(14));
        initLbl.setForeground(TEXT_WHITE);
        avatarCircle.add(initLbl);

        JPanel userCol = new JPanel();
        userCol.setLayout(new BoxLayout(userCol, BoxLayout.Y_AXIS));
        userCol.setOpaque(false);
        JLabel roleMini = new JLabel(isGuest ? "GUEST" : "MEMBER ID");
        roleMini.setFont(UITheme.fontBold(9));
        roleMini.setForeground(ACCENT_GLOW);
        JLabel userLbl = new JLabel(displayName);
        userLbl.setFont(UITheme.fontBold(13));
        userLbl.setForeground(TEXT_WHITE);
        userCol.add(roleMini);
        userCol.add(userLbl);

        UITheme.GradientButton logoutBtn = UITheme.GradientButton.createGhost(
                isGuest ? "Log In / Sign Up" : "Logout",
                ACCENT_LIGHT,
                16
        );
        logoutBtn.setPreferredSize(new Dimension(isGuest ? 138 : 94, 36));
        logoutBtn.setFont(UITheme.fontBold(12));
        logoutBtn.addActionListener(e -> {
            this.currentUser = null;
            dispose();
            SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
        });

        rightBox.add(avatarCircle);
        rightBox.add(userCol);
        rightBox.add(Box.createRigidArea(new Dimension(4, 0)));
        rightBox.add(logoutBtn);

        banner.add(leftBox, BorderLayout.WEST);
        banner.add(rightBox, BorderLayout.EAST);
        return banner;
    }

    private void showMyTicketsWalletDialog() {
        if (bookedTickets.isEmpty()) {
            ThemedDialog.showInfo(
                    this,
                    "My E-Tickets",
                    "You haven't booked any tickets in this session yet. Select a movie below to reserve seats!"
            );
            return;
        }

        JDialog walletDialog = new JDialog(this, "My E-Tickets Wallet (" + bookedTickets.size() + ")", true);
        walletDialog.setSize(640, 520);
        walletDialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(0, 14));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        JPanel topHeader = new JPanel(new BorderLayout());
        topHeader.setOpaque(false);
        JLabel title = new JLabel("My Booked E-Tickets (" + bookedTickets.size() + ")");
        title.setFont(UITheme.fontDisplay(20));
        title.setForeground(TEXT_WHITE);
        JLabel sub = new JLabel("Click 'View E-Ticket' on any booking to open your boarding pass");
        sub.setFont(UITheme.fontRegular(12));
        sub.setForeground(TEXT_MUTED);

        JPanel titleCol = new JPanel();
        titleCol.setLayout(new BoxLayout(titleCol, BoxLayout.Y_AXIS));
        titleCol.setOpaque(false);
        titleCol.add(title);
        titleCol.add(Box.createRigidArea(new Dimension(0, 3)));
        titleCol.add(sub);
        topHeader.add(titleCol, BorderLayout.WEST);

        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG_DARK);

        for (int i = bookedTickets.size() - 1; i >= 0; i--) {
            BookedTicketRecord rec = bookedTickets.get(i);

            UITheme.RoundedPanel rowCard = new UITheme.RoundedPanel(16, PANEL_BG, UITheme.BORDER_SUBTLE, true);
            rowCard.enableHoverEffect(ACCENT_LIGHT);
            rowCard.setLayout(new BorderLayout(14, 0));
            rowCard.setBorder(new EmptyBorder(14, 16, 14, 16));
            rowCard.setMaximumSize(new Dimension(Integer.MAX_VALUE, 96));

            JPanel leftInfo = new JPanel();
            leftInfo.setLayout(new BoxLayout(leftInfo, BoxLayout.Y_AXIS));
            leftInfo.setOpaque(false);

            JLabel movieLbl = new JLabel(rec.movieTitle + "   •   " + rec.refCode);
            movieLbl.setFont(UITheme.fontBold(15));
            movieLbl.setForeground(TEXT_WHITE);

            JLabel dateSeatsLbl = new JLabel(rec.showDate + "  |  " + rec.showTime + "  |  Seats: " + rec.seatsSummary);
            dateSeatsLbl.setFont(UITheme.fontRegular(12));
            dateSeatsLbl.setForeground(TEXT_MUTED);

            JLabel paidLbl = new JLabel("Paid: " + rec.totalPaid);
            paidLbl.setFont(UITheme.fontBold(12));
            paidLbl.setForeground(ACCENT_GOLD);

            leftInfo.add(movieLbl);
            leftInfo.add(Box.createRigidArea(new Dimension(0, 4)));
            leftInfo.add(dateSeatsLbl);
            leftInfo.add(Box.createRigidArea(new Dimension(0, 4)));
            leftInfo.add(paidLbl);

            UITheme.GradientButton viewBtn = new UITheme.GradientButton("View E-Ticket", ACCENT_PURPLE, ACCENT_PINK, 12);
            viewBtn.setPreferredSize(new Dimension(132, 36));
            viewBtn.setFont(UITheme.fontBold(12));
            viewBtn.addActionListener(ev -> rec.openTicketDialog.run());

            JPanel btnWrap = new JPanel(new GridBagLayout());
            btnWrap.setOpaque(false);
            btnWrap.add(viewBtn);

            rowCard.add(leftInfo, BorderLayout.CENTER);
            rowCard.add(btnWrap, BorderLayout.EAST);

            listPanel.add(rowCard);
            listPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        }

        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_DARK);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(8, 0));
        scroll.getVerticalScrollBar().setUI(new SlimPurpleScrollBarUI());

        UITheme.GradientButton closeBtn = UITheme.GradientButton.createGhost("Close Wallet", UITheme.BORDER_SUBTLE, 14);
        closeBtn.setPreferredSize(new Dimension(140, 38));
        closeBtn.addActionListener(ev -> walletDialog.dispose());
        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomBar.setOpaque(false);
        bottomBar.add(closeBtn);

        root.add(topHeader, BorderLayout.NORTH);
        root.add(scroll, BorderLayout.CENTER);
        root.add(bottomBar, BorderLayout.SOUTH);

        walletDialog.setContentPane(UITheme.Motion.wrapWithEntrance(root, 20, 240));
        walletDialog.setVisible(true);
    }

    // 2. ANIMATED SLIDING PILL FILTER BAR + SORT & AVAILABILITY CONTROLS
    private static class SlidingTabBar extends JPanel {
        private final List<JButton> tabs = new ArrayList<>();
        private int selectedIndex = 0;
        private float pillX = 0f;
        private float pillW = 0f;
        private Color startColor = ACCENT_PURPLE;
        private Color endColor = ACCENT_PINK;
        private Timer slideTimer;

        public SlidingTabBar() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
            setOpaque(false);
        }

        public JButton addTab(String text, int width) {
            int idx = tabs.size();
            JButton btn = new JButton(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    if (selectedIndex != idx) {
                        g2.setColor(new Color(34, 26, 62, 160));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                        g2.setColor(UITheme.BORDER_SUBTLE);
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                    }
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            btn.setPreferredSize(new Dimension(width, 36));
            btn.setFont(UITheme.fontBold(12));
            btn.setForeground(TEXT_WHITE);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
            tabs.add(btn);
            add(btn);
            return btn;
        }

        public void selectTab(int index, Color c1, Color c2) {
            if (index < 0 || index >= tabs.size()) return;
            selectedIndex = index;
            JButton target = tabs.get(index);

            if (pillW <= 1f) {
                pillX = target.getX();
                pillW = target.getWidth();
                startColor = c1;
                endColor = c2;
                repaint();
                return;
            }

            if (slideTimer != null && slideTimer.isRunning()) slideTimer.stop();
            final float fromX = pillX;
            final float fromW = pillW;
            final float toX = target.getX();
            final float toW = target.getWidth();
            final Color fromC1 = startColor;
            final Color fromC2 = endColor;
            final long start = System.currentTimeMillis();
            final int duration = 240;

            slideTimer = new Timer(15, e -> {
                float raw = Math.min(1f, (System.currentTimeMillis() - start) / (float) duration);
                float ease = 1f - (float) Math.pow(1f - raw, 3);
                pillX = fromX + (toX - fromX) * ease;
                pillW = fromW + (toW - fromW) * ease;
                startColor = blendColor(fromC1, c1, ease);
                endColor = blendColor(fromC2, c2, ease);
                repaint();
                if (raw >= 1f) {
                    pillX = toX;
                    pillW = toW;
                    startColor = c1;
                    endColor = c2;
                    slideTimer.stop();
                }
            });
            slideTimer.start();
        }

        private Color blendColor(Color a, Color b, float t) {
            int r = Math.round(a.getRed() + (b.getRed() - a.getRed()) * t);
            int g = Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t);
            int bl = Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t);
            return new Color(r, g, bl);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!tabs.isEmpty()) {
                JButton activeBtn = tabs.get(selectedIndex);
                if (pillW <= 1f && activeBtn.getWidth() > 0) {
                    pillX = activeBtn.getX();
                    pillW = activeBtn.getWidth();
                }
                if (pillW > 1f) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    int y = activeBtn.getY();
                    int h = activeBtn.getHeight();
                    GradientPaint gp = new GradientPaint(pillX, y, startColor, pillX + pillW, y + h, endColor);
                    g2.setPaint(gp);
                    g2.fillRoundRect(Math.round(pillX), y, Math.round(pillW), h, 18, 18);
                    g2.setColor(new Color(255, 255, 255, 110));
                    g2.drawRoundRect(Math.round(pillX), y, Math.round(pillW) - 1, h - 1, 18, 18);
                    g2.dispose();
                }
            }
        }
    }

    private JPanel createFilterAndSearchBar() {
        UITheme.RoundedPanel toolbar = new UITheme.RoundedPanel(18, new Color(28, 22, 54, 225), UITheme.BORDER_SUBTLE, false);
        toolbar.setLayout(new BorderLayout(12, 0));
        toolbar.setBorder(new EmptyBorder(8, 14, 8, 14));
        toolbar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 58));
        toolbar.setAlignmentX(Component.LEFT_ALIGNMENT);

        int regCount = cinemaManager.getRegularGroups().size();
        int promoCount = cinemaManager.getPromoGroups().size();
        int totalCount = regCount + promoCount;

        SlidingTabBar slidingTabs = new SlidingTabBar();
        JButton allChip = slidingTabs.addTab("All Movies (" + totalCount + ")", 128);
        JButton regularChip = slidingTabs.addTab("Now Showing (" + regCount + ")", 148);
        JButton promoChip = slidingTabs.addTab("Holiday Promos (" + promoCount + ")", 156);

        allChip.addActionListener(e -> {
            activeFilter = "ALL";
            slidingTabs.selectTab(0, ACCENT_PURPLE, ACCENT_PINK);
            refreshFilteredMovies(true);
        });

        regularChip.addActionListener(e -> {
            activeFilter = "REGULAR";
            slidingTabs.selectTab(1, ACCENT_PURPLE, ACCENT_PINK);
            refreshFilteredMovies(true);
        });

        promoChip.addActionListener(e -> {
            activeFilter = "PROMO";
            slidingTabs.selectTab(2, UITheme.PROMO_RED, UITheme.PROMO_GREEN);
            refreshFilteredMovies(true);
        });

        // Right Controls: Sort Cycle Pill + Available Only Pill + Search Box
        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        UITheme.GradientButton sortBtn = UITheme.GradientButton.createGhost("Sort: Featured", ACCENT_LIGHT, 16);
        sortBtn.setPreferredSize(new Dimension(146, 38));
        sortBtn.setFont(UITheme.fontBold(11));
        sortBtn.setToolTipText("Click to cycle sort order");
        sortBtn.addActionListener(e -> {
            if (sortMode.equals("FEATURED")) {
                sortMode = "PRICE_ASC";
                sortBtn.setText("Sort: Lowest Price");
            } else if (sortMode.equals("PRICE_ASC")) {
                sortMode = "SEATS_DESC";
                sortBtn.setText("Sort: Most Seats");
            } else {
                sortMode = "FEATURED";
                sortBtn.setText("Sort: Featured");
            }
            refreshFilteredMovies(true);
        });

        UITheme.GradientButton availBtn = UITheme.GradientButton.createGhost("Available Only", UITheme.BORDER_SUBTLE, 16);
        availBtn.setPreferredSize(new Dimension(124, 38));
        availBtn.setFont(UITheme.fontBold(11));
        availBtn.addActionListener(e -> {
            availableOnly = !availableOnly;
            if (availableOnly) {
                availBtn.setGradientColors(new Color(16, 120, 88), new Color(16, 185, 129));
                availBtn.setForeground(TEXT_WHITE);
            } else {
                availBtn.setGradientColors(new Color(0, 0, 0, 0), new Color(0, 0, 0, 0));
                availBtn.setForeground(TEXT_MUTED);
            }
            refreshFilteredMovies(true);
        });

        UITheme.IconTextField searchBox = new UITheme.IconTextField(
                "Search movies...",
                UITheme.VectorIcon.Type.SEARCH
        );
        searchBox.setPreferredSize(new Dimension(250, 40));
        searchBox.getField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearch(); }
            private void onSearch() {
                searchQuery = searchBox.getText().trim().toLowerCase();
                refreshFilteredMovies(false);
            }
        });

        rightControls.add(sortBtn);
        rightControls.add(availBtn);
        rightControls.add(searchBox);

        toolbar.add(slidingTabs, BorderLayout.WEST);
        toolbar.add(rightControls, BorderLayout.EAST);
        return toolbar;
    }

    private void refreshFilteredMovies(boolean animateCards) {
        boolean showFeatured = activeFilter.equals("ALL") && searchQuery.isEmpty() && sortMode.equals("FEATURED") && !availableOnly;
        if (featuredSectionWrapper != null) {
            featuredSectionWrapper.setExpandedAnimated(showFeatured);
        }

        boolean showRegular = activeFilter.equals("ALL") || activeFilter.equals("REGULAR");
        boolean showPromo = activeFilter.equals("ALL") || activeFilter.equals("PROMO");

        regularSectionWrapper.setVisible(showRegular);
        promoSectionWrapper.setVisible(showPromo);

        int cols = getWidth() >= 1450 ? 4 : (getWidth() < 1050 ? 2 : 3);

        if (showRegular) {
            rebuildFilteredGrid(regularGridPanel, cinemaManager.getRegularGroups(), false, cols, animateCards);
        }
        if (showPromo) {
            rebuildFilteredGrid(promoGridPanel, cinemaManager.getPromoGroups(), true, cols, animateCards);
        }

        revalidate();
        repaint();
    }

    private List<MovieGroup> filterAndSortMovies(List<MovieGroup> groups) {
        List<MovieGroup> list = new ArrayList<>();
        for (MovieGroup g : groups) {
            if (!searchQuery.isEmpty() && !g.getTitle().toLowerCase().contains(searchQuery)) {
                continue;
            }
            if (availableOnly) {
                LocalDate[] range = parseMovieDateRange(g.getDateRange());
                if (getAvailableSeatCount(g.getTitle(), range[0], g.getTime()) <= 0) {
                    continue;
                }
            }
            list.add(g);
        }

        if (sortMode.equals("PRICE_ASC")) {
            list.sort(Comparator.comparingDouble(MovieGroup::getPrice));
        } else if (sortMode.equals("SEATS_DESC")) {
            list.sort((a, b) -> {
                LocalDate[] ra = parseMovieDateRange(a.getDateRange());
                LocalDate[] rb = parseMovieDateRange(b.getDateRange());
                int sa = getAvailableSeatCount(a.getTitle(), ra[0], a.getTime());
                int sb = getAvailableSeatCount(b.getTitle(), rb[0], b.getTime());
                return Integer.compare(sb, sa);
            });
        }
        return list;
    }

    private void rebuildFilteredGrid(JPanel gridPanel, List<MovieGroup> groups, boolean isPromo, int cols, boolean animateCards) {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(0, cols, 18, 18));

        List<MovieGroup> processed = filterAndSortMovies(groups);
        int added = 0;
        for (MovieGroup g : processed) {
            MovieCard card = new MovieCard(g, isPromo);
            if (animateCards) {
                card.playEntrance(added * 45, 14, 260);
            }
            gridPanel.add(card);
            added++;
        }

        if (added == 0) {
            gridPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel empty = new JLabel("No movies match your current search or filter criteria.");
            empty.setFont(UITheme.fontRegular(14));
            empty.setForeground(TEXT_MUTED);
            gridPanel.add(empty);
        }
    }

    // 3. DUAL-LAYER CROSS-DISSOLVE SPOTLIGHT STAGE + ANIMATED PROGRESS BAR
    private static class CrossDissolveStage extends JPanel {
        private BufferedImage outgoingSnapshot;
        private float dissolveAlpha = 1.0f;
        private int slideOffset = 0;
        private Timer dissolveTimer;

        public CrossDissolveStage() {
            super(new BorderLayout());
            setOpaque(false);
        }

        public void captureOutgoingFrame() {
            if (getWidth() > 0 && getHeight() > 0 && getComponentCount() > 0) {
                outgoingSnapshot = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = outgoingSnapshot.createGraphics();
                super.paint(g2);
                g2.dispose();
            }
        }

        public void startDissolve(int directionHint) {
            if (dissolveTimer != null && dissolveTimer.isRunning()) {
                dissolveTimer.stop();
            }
            dissolveAlpha = 0f;
            slideOffset = directionHint * 18;
            final long start = System.currentTimeMillis();
            final int duration = 340;

            dissolveTimer = new Timer(15, e -> {
                float raw = Math.min(1f, (System.currentTimeMillis() - start) / (float) duration);
                float ease = 1f - (float) Math.pow(1f - raw, 3);
                dissolveAlpha = ease;
                slideOffset = Math.round(directionHint * 18 * (1f - ease));
                repaint();
                if (raw >= 1f) {
                    dissolveAlpha = 1f;
                    slideOffset = 0;
                    outgoingSnapshot = null;
                    dissolveTimer.stop();
                }
            });
            dissolveTimer.start();
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            if (outgoingSnapshot != null && dissolveAlpha < 0.999f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f - dissolveAlpha));
                g2.drawImage(outgoingSnapshot, -slideOffset / 2, 0, null);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, dissolveAlpha));
                g2.translate(slideOffset, 0);
            }
            super.paint(g2);
            g2.dispose();
        }
    }

    private JPanel createTopBrowsingSection(List<MovieGroup> movies) {
        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        if (movies == null || movies.isEmpty()) {
            return container;
        }

int[] spotlightIds = {9, 7, 10, 1, 4}; // Put your chosen MovieGroup IDs here

final List<MovieGroup> spotlights = new ArrayList<>();
for (int targetId : spotlightIds) {
    for (MovieGroup m : movies) {
        if (m.getId() == targetId) {
            spotlights.add(m);
            break;
        }
    }
}
if (spotlights.isEmpty()) {
    spotlights.addAll(movies.subList(0, Math.min(5, movies.size())));
}

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleWithBar = createMovieSectionTitle(
                "Featured Spotlight",
                "Now Premiering",
                ACCENT_PURPLE,
                ACCENT_PINK
        );

        JPanel navControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 2));
        navControls.setOpaque(false);

        JPanel dotsRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 8));
        dotsRow.setOpaque(false);

        UITheme.GradientButton prevHeroBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 18);
        prevHeroBtn.setPreferredSize(new Dimension(38, 32));
        prevHeroBtn.setIcon(new ChevronArrowIcon(true, 11, TEXT_WHITE));
        prevHeroBtn.setToolTipText("Previous Spotlight");

        UITheme.GradientButton nextHeroBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 18);
        nextHeroBtn.setPreferredSize(new Dimension(38, 32));
        nextHeroBtn.setIcon(new ChevronArrowIcon(false, 11, TEXT_WHITE));
        nextHeroBtn.setToolTipText("Next Spotlight");

        navControls.add(dotsRow);
        navControls.add(Box.createRigidArea(new Dimension(4, 0)));
        navControls.add(prevHeroBtn);
        navControls.add(nextHeroBtn);

        headerPanel.add(titleWithBar, BorderLayout.WEST);
        headerPanel.add(navControls, BorderLayout.EAST);
        container.add(headerPanel, BorderLayout.NORTH);

        CrossDissolveStage heroStage = new CrossDissolveStage();
        heroStage.setPreferredSize(new Dimension(0, 316));
        heroStage.setMaximumSize(new Dimension(Integer.MAX_VALUE, 316));

        java.util.function.BiConsumer<Boolean, Integer> renderHero = (animateTransition, dirHint) -> {
            if (animateTransition) {
                heroStage.captureOutgoingFrame();
            }
            heroStage.removeAll();
            heroProgress = 0f;

            int safeIdx = ((heroIndex % spotlights.size()) + spotlights.size()) % spotlights.size();
            heroIndex = safeIdx;
            MovieGroup featured = spotlights.get(safeIdx);
            boolean isPromo = featured.getTitle().contains("[CHRISTMAS PROMO]") || featured.getPrice() <= 110.0;
            String cleanTitle = featured.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
            int cinemaNo = (Math.abs(featured.getId()) % 4) + 1;
            LocalDate[] range = parseMovieDateRange(featured.getDateRange());
            int seatsLeft = getAvailableSeatCount(featured.getTitle(), range[0], featured.getTime());

            ImageIcon bgIcon = loadPoster(featured.getPosterPath(), 900, 316, 22);
            Image bgImg = (bgIcon != null) ? bgIcon.getImage() : null;

            JPanel heroCard = new JPanel(new BorderLayout(24, 0)) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    int w = getWidth();
                    int h = getHeight();

                    RoundRectangle2D clip = new RoundRectangle2D.Float(0, 0, w, h, 22, 22);
                    g2.setClip(clip);

                    g2.setColor(CARD_BG);
                    g2.fillRect(0, 0, w, h);

                    if (bgImg != null) {
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f));
                        g2.drawImage(bgImg, w / 3, -30, (w * 2) / 3 + 40, h + 60, null);
                        g2.setComposite(AlphaComposite.SrcOver);
                    }

                    GradientPaint overlay = new GradientPaint(
                            0, 0, new Color(24, 18, 48, 248),
                            w, 0, new Color(36, 22, 74, 185)
                    );
                    g2.setPaint(overlay);
                    g2.fillRect(0, 0, w, h);

                    // Animated Auto-Advance Progress Bar at bottom of Hero Card
                    int barW = Math.round(w * Math.max(0f, Math.min(1f, heroProgress)));
                    if (barW > 0) {
                        GradientPaint progGp = new GradientPaint(0, h - 3, ACCENT_PURPLE, w, h - 3, ACCENT_GOLD);
                        g2.setPaint(progGp);
                        g2.fillRect(0, h - 3, barW, 3);
                    }

                    g2.setClip(null);
                    GradientPaint borderGp = new GradientPaint(
                            0, 0, isPromo ? UITheme.PROMO_RED : ACCENT_LIGHT,
                            w, h, isPromo ? UITheme.PROMO_GREEN : ACCENT_PINK
                    );
                    g2.setPaint(borderGp);
                    g2.setStroke(new BasicStroke(1.4f));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, 22, 22);
                    g2.dispose();
                }
            };
            heroCard.setOpaque(false);
            heroCard.setBorder(new EmptyBorder(20, 24, 20, 26));

            JPanel posterFrame = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    GradientPaint gp = new GradientPaint(0, 0, ACCENT_LIGHT, getWidth(), getHeight(), ACCENT_PINK);
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
                    g2.dispose();
                }
            };
            posterFrame.setOpaque(false);
            posterFrame.setBorder(new EmptyBorder(2, 2, 2, 2));
            posterFrame.setPreferredSize(new Dimension(184, 272));

            JLabel posterLbl;
            ImageIcon posterIcon = loadPoster(featured.getPosterPath(), 180, 268, 16);
            if (posterIcon != null) {
                posterLbl = new JLabel(posterIcon);
            } else {
                posterLbl = new JLabel("<html><center><b>SINE-SERVE</b><br>" + cleanTitle + "</center></html>", SwingConstants.CENTER);
                posterLbl.setPreferredSize(new Dimension(180, 268));
                posterLbl.setOpaque(true);
                posterLbl.setBackground(PANEL_BG);
                posterLbl.setForeground(TEXT_MUTED);
            }
            posterFrame.add(posterLbl, BorderLayout.CENTER);

            JPanel infoCol = new JPanel();
            infoCol.setLayout(new BoxLayout(infoCol, BoxLayout.Y_AXIS));
            infoCol.setOpaque(false);

            JPanel badgeStrip = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            badgeStrip.setOpaque(false);
            badgeStrip.setAlignmentX(Component.LEFT_ALIGNMENT);
            badgeStrip.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            UITheme.PillLabel spotlightPill = new UITheme.PillLabel(
                    isPromo ? "HOLIDAY SPECIAL" : "PREMIERE SPOTLIGHT  #" + (safeIdx + 1),
                    isPromo ? new Color(225, 29, 72, 65) : new Color(124, 58, 237, 65),
                    isPromo ? new Color(252, 165, 165) : ACCENT_GLOW,
                    isPromo ? UITheme.PROMO_RED : ACCENT_LIGHT
            );
            spotlightPill.setFont(UITheme.fontBold(10));

            UITheme.PillLabel cinemaPill = new UITheme.PillLabel(
                    "CINEMA " + cinemaNo + "  •  HOT",
                    new Color(34, 26, 62, 210),
                    TEXT_WHITE,
                    UITheme.BORDER_SUBTLE
            );
            cinemaPill.setFont(UITheme.fontBold(10));

            UITheme.PillLabel seatsPill = new UITheme.PillLabel(
                    seatsLeft + " / 90 Seats Available",
                    new Color(16, 185, 129, 38),
                    new Color(110, 231, 183),
                    UITheme.PROMO_GREEN
            );
            seatsPill.setFont(UITheme.fontBold(10));

            badgeStrip.add(spotlightPill);
            badgeStrip.add(cinemaPill);
            badgeStrip.add(seatsPill);

            JLabel heroTitle = new JLabel(cleanTitle);
            heroTitle.setFont(UITheme.fontDisplay(27));
            heroTitle.setForeground(TEXT_WHITE);
            heroTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

            JTextArea heroSynopsis = new JTextArea(featured.getDescription());
            heroSynopsis.setWrapStyleWord(true);
            heroSynopsis.setLineWrap(true);
            heroSynopsis.setEditable(false);
            heroSynopsis.setFocusable(false);
            heroSynopsis.setOpaque(false);
            heroSynopsis.setBorder(null);
            heroSynopsis.setFont(UITheme.fontRegular(13));
            heroSynopsis.setForeground(new Color(214, 219, 230));
            heroSynopsis.setMaximumSize(new Dimension(690, 62));
            heroSynopsis.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel scheduleMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
            scheduleMeta.setOpaque(false);
            scheduleMeta.setAlignmentX(Component.LEFT_ALIGNMENT);
            scheduleMeta.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

            JLabel dateMeta = new JLabel(
                    " " + featured.getDateRange(),
                    new UITheme.VectorIcon(UITheme.VectorIcon.Type.CALENDAR, 14, ACCENT_GLOW),
                    SwingConstants.LEFT
            );
            dateMeta.setFont(UITheme.fontBold(12));
            dateMeta.setForeground(TEXT_WHITE);

            JLabel timeMeta = new JLabel(
                    " " + featured.getTime(),
                    new UITheme.VectorIcon(UITheme.VectorIcon.Type.CLOCK, 14, ACCENT_GLOW),
                    SwingConstants.LEFT
            );
            timeMeta.setFont(UITheme.fontBold(12));
            timeMeta.setForeground(TEXT_WHITE);

            UITheme.PillLabel heroPricePill = new UITheme.PillLabel(
                    "PHP " + String.format("%.2f", featured.getPrice()) + " / ticket",
                    new Color(251, 191, 36, 48),
                    ACCENT_GOLD,
                    new Color(251, 191, 36, 160)
            );
            heroPricePill.setFont(UITheme.fontBold(12));

            scheduleMeta.add(dateMeta);
            scheduleMeta.add(timeMeta);
            scheduleMeta.add(heroPricePill);

            JPanel heroActions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
            heroActions.setOpaque(false);
            heroActions.setAlignmentX(Component.LEFT_ALIGNMENT);
            heroActions.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

            UITheme.GradientButton bookHeroBtn;
            if (isGuest) {
                bookHeroBtn = UITheme.GradientButton.createGhost("Log In to Reserve", ACCENT_LIGHT, 14);
                bookHeroBtn.setPreferredSize(new Dimension(190, 42));
                bookHeroBtn.addActionListener(e -> ThemedDialog.showInfo(
                        CinemaUI.this,
                        "Login Required",
                        "Guest mode is browse-only. Please log in or create an account to reserve tickets."
                ));
            } else {
                bookHeroBtn = new UITheme.GradientButton(
                        "Reserve Seats Now",
                        isPromo ? UITheme.PROMO_RED : ACCENT_PURPLE,
                        isPromo ? UITheme.PROMO_GREEN : ACCENT_PINK,
                        14
                );
                bookHeroBtn.setPreferredSize(new Dimension(224, 42));
                bookHeroBtn.setFont(UITheme.fontBold(13));
                bookHeroBtn.addActionListener(e -> startBookingFlow(featured, null, 1, false, "Cash / OTC"));
            }

            UITheme.GradientButton previewHeroBtn = UITheme.GradientButton.createGhost("Quick Preview", UITheme.BORDER_SUBTLE, 14);
            previewHeroBtn.setPreferredSize(new Dimension(142, 42));
            previewHeroBtn.setFont(UITheme.fontBold(12));
            previewHeroBtn.addActionListener(e -> showMovieQuickPreviewDialog(featured, isPromo));

            heroActions.add(bookHeroBtn);
            heroActions.add(previewHeroBtn);

            infoCol.add(badgeStrip);
            infoCol.add(Box.createRigidArea(new Dimension(0, 10)));
            infoCol.add(heroTitle);
            infoCol.add(Box.createRigidArea(new Dimension(0, 8)));
            infoCol.add(heroSynopsis);
            infoCol.add(Box.createRigidArea(new Dimension(0, 12)));
            infoCol.add(scheduleMeta);
            infoCol.add(Box.createVerticalGlue());
            infoCol.add(heroActions);

            heroCard.add(posterFrame, BorderLayout.WEST);
            heroCard.add(infoCol, BorderLayout.CENTER);

            heroStage.add(heroCard, BorderLayout.CENTER);
            heroStage.revalidate();
            heroStage.repaint();

            if (animateTransition) {
                heroStage.startDissolve(dirHint);
            }
        };

        Runnable rebuildDots = new Runnable() {
            @Override
            public void run() {
                dotsRow.removeAll();
                int safeIdx = ((heroIndex % spotlights.size()) + spotlights.size()) % spotlights.size();
                for (int i = 0; i < spotlights.size(); i++) {
                    final int idx = i;
                    boolean active = (i == safeIdx);
                    JButton dot = new JButton() {
                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            UITheme.enableAntiAliasing(g2);
                            if (active) {
                                GradientPaint gp = new GradientPaint(0, 0, ACCENT_PURPLE, getWidth(), 0, ACCENT_PINK);
                                g2.setPaint(gp);
                            } else {
                                g2.setColor(new Color(95, 82, 138));
                            }
                            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                            g2.dispose();
                        }
                    };
                    dot.setPreferredSize(new Dimension(active ? 28 : 9, 8));
                    dot.setContentAreaFilled(false);
                    dot.setBorderPainted(false);
                    dot.setFocusPainted(false);
                    dot.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    dot.addActionListener(e -> {
                        if (heroIndex != idx) {
                            int dir = idx > heroIndex ? 1 : -1;
                            heroIndex = idx;
                            renderHero.accept(true, dir);
                            run();
                        }
                    });
                    dotsRow.add(dot);
                }
                dotsRow.revalidate();
                dotsRow.repaint();
            }
        };

        prevHeroBtn.addActionListener(e -> {
            heroIndex--;
            renderHero.accept(true, -1);
            rebuildDots.run();
        });
        nextHeroBtn.addActionListener(e -> {
            heroIndex++;
            renderHero.accept(true, 1);
            rebuildDots.run();
        });

        // Smooth 60fps Progress Bar & Auto-Advance every 6.5 seconds
        if (heroProgressTimer != null) heroProgressTimer.stop();
        heroProgressTimer = new Timer(40, e -> {
            if (isActive() && featuredSectionWrapper != null && featuredSectionWrapper.isExpanded() && heroStage.getMousePosition() == null) {
                heroProgress += 40f / HERO_INTERVAL_MS;
                if (heroProgress >= 1f) {
                    heroProgress = 0f;
                    heroIndex++;
                    renderHero.accept(true, 1);
                    rebuildDots.run();
                } else {
                    heroStage.repaint();
                }
            }
        });
        heroProgressTimer.start();

        renderHero.accept(false, 1);
        rebuildDots.run();
        container.add(heroStage, BorderLayout.CENTER);
        return container;
    }

    // INTERACTIVE QUICK PREVIEW MODAL (FOR GUESTS & MEMBERS)
    private void showMovieQuickPreviewDialog(MovieGroup group, boolean isPromo) {
        hideSynopsisPopover();
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        int cinemaNo = (Math.abs(group.getId()) % 4) + 1;
        LocalDate[] range = parseMovieDateRange(group.getDateRange());
        int seatsLeft = getAvailableSeatCount(group.getTitle(), range[0], group.getTime());

        JDialog previewDialog = new JDialog(this, "Movie Details • " + cleanTitle, true);
        previewDialog.setSize(680, 440);
        previewDialog.setLocationRelativeTo(this);

        JPanel root = new JPanel(new BorderLayout(20, 16));
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel posterLbl;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 184, 276, 16);
        if (posterIcon != null) {
            posterLbl = new JLabel(posterIcon);
        } else {
            posterLbl = new JLabel("<html><center><b>SINE-SERVE</b><br>" + cleanTitle + "</center></html>", SwingConstants.CENTER);
            posterLbl.setPreferredSize(new Dimension(184, 276));
            posterLbl.setOpaque(true);
            posterLbl.setBackground(PANEL_BG);
            posterLbl.setForeground(TEXT_MUTED);
        }

        JPanel rightInfo = new JPanel();
        rightInfo.setLayout(new BoxLayout(rightInfo, BoxLayout.Y_AXIS));
        rightInfo.setOpaque(false);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        badges.setOpaque(false);
        badges.setAlignmentX(Component.LEFT_ALIGNMENT);
        badges.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));

        badges.add(new UITheme.PillLabel(
                isPromo ? "CHRISTMAS PROMO" : "STANDARD 2D",
                isPromo ? new Color(225, 29, 72, 55) : new Color(124, 58, 237, 55),
                isPromo ? new Color(252, 165, 165) : ACCENT_GLOW,
                isPromo ? UITheme.PROMO_RED : ACCENT_PURPLE
        ));
        badges.add(new UITheme.PillLabel(
                "CINEMA " + cinemaNo,
                new Color(34, 26, 62),
                TEXT_WHITE,
                UITheme.BORDER_SUBTLE
        ));
        badges.add(new UITheme.PillLabel(
                seatsLeft + " / 90 Seats Open",
                new Color(16, 185, 129, 38),
                new Color(110, 231, 183),
                UITheme.PROMO_GREEN
        ));

        JLabel titleLbl = new JLabel(cleanTitle);
        titleLbl.setFont(UITheme.fontDisplay(22));
        titleLbl.setForeground(TEXT_WHITE);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea fullSynopsis = new JTextArea(group.getDescription());
        fullSynopsis.setWrapStyleWord(true);
        fullSynopsis.setLineWrap(true);
        fullSynopsis.setEditable(false);
        fullSynopsis.setFocusable(false);
        fullSynopsis.setOpaque(false);
        fullSynopsis.setBorder(null);
        fullSynopsis.setFont(UITheme.fontRegular(13));
        fullSynopsis.setForeground(new Color(214, 219, 230));
        fullSynopsis.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel schedLbl = new JLabel("Screening Dates:  " + group.getDateRange() + "   •   " + group.getTime());
        schedLbl.setFont(UITheme.fontBold(12));
        schedLbl.setForeground(ACCENT_GLOW);
        schedLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLbl = new JLabel("Ticket Rate:  PHP " + String.format("%.2f", group.getPrice()) + " per seat");
        priceLbl.setFont(UITheme.fontBold(14));
        priceLbl.setForeground(ACCENT_GOLD);
        priceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        rightInfo.add(badges);
        rightInfo.add(Box.createRigidArea(new Dimension(0, 10)));
        rightInfo.add(titleLbl);
        rightInfo.add(Box.createRigidArea(new Dimension(0, 10)));
        rightInfo.add(fullSynopsis);
        rightInfo.add(Box.createVerticalGlue());
        rightInfo.add(schedLbl);
        rightInfo.add(Box.createRigidArea(new Dimension(0, 6)));
        rightInfo.add(priceLbl);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        UITheme.GradientButton closeBtn = UITheme.GradientButton.createGhost("Close", UITheme.BORDER_SUBTLE, 14);
        closeBtn.setPreferredSize(new Dimension(110, 40));
        closeBtn.addActionListener(e -> previewDialog.dispose());

        UITheme.GradientButton ctaBtn;
        if (isGuest) {
            ctaBtn = new UITheme.GradientButton("Sign In to Book", ACCENT_PURPLE, ACCENT_PINK, 14);
            ctaBtn.setPreferredSize(new Dimension(190, 40));
            ctaBtn.addActionListener(e -> {
                previewDialog.dispose();
                this.currentUser = null;
                dispose();
                SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
            });
        } else {
            ctaBtn = new UITheme.GradientButton("Reserve Seats", ACCENT_PURPLE, ACCENT_PINK, 14);
            ctaBtn.setPreferredSize(new Dimension(190, 40));
            ctaBtn.addActionListener(e -> {
                previewDialog.dispose();
                startBookingFlow(group, null, 1, false, "Cash / OTC");
            });
        }

        footer.add(closeBtn);
        footer.add(ctaBtn);

        root.add(posterLbl, BorderLayout.WEST);
        root.add(rightInfo, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        previewDialog.setContentPane(UITheme.Motion.wrapWithEntrance(root, 20, 240));
        previewDialog.setVisible(true);
    }

    private static class ChevronArrowIcon implements Icon {
        private final boolean left;
        private final int size;
        private final Color color;

        public ChevronArrowIcon(boolean left, int size, Color color) {
            this.left = left;
            this.size = size;
            this.color = color;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int midY = y + size / 2;
            int offset = size / 3;
            Path2D path = new Path2D.Float();
            if (left) {
                path.moveTo(x + size / 2f + offset / 2f, midY - offset);
                path.lineTo(x + size / 2f - offset / 2f, midY);
                path.lineTo(x + size / 2f + offset / 2f, midY + offset);
            } else {
                path.moveTo(x + size / 2f - offset / 2f, midY - offset);
                path.lineTo(x + size / 2f + offset / 2f, midY);
                path.lineTo(x + size / 2f - offset / 2f, midY + offset);
            }
            g2.draw(path);
            g2.dispose();
        }
    }

    private static class SlimPurpleScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = ACCENT_PURPLE;
            this.trackColor = TRACK_BG;
        }

        @Override protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            g2.setColor(TRACK_BG);
            g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 8, 8);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            Color cThumb = isDragging ? ACCENT_PINK : (isThumbRollover() ? ACCENT_LIGHT : ACCENT_PURPLE);
            g2.setColor(cThumb);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 8, 8);
            g2.dispose();
        }
    }

    private static class DescriptionComponent extends JComponent {
        private final String text;
        private final Font font;
        private final Color color;
        private final int maxLines;
        private boolean truncated = false;

        DescriptionComponent(String text, Font font, Color color, int maxLines) {
            this.text = text == null ? "" : text;
            this.font = font;
            this.color = color;
            this.maxLines = maxLines;
            setOpaque(false);
            int lineStep = getFontMetrics(font).getHeight() + 2;
            int h = lineStep * maxLines + 2;
            setPreferredSize(new Dimension(120, h));
            setMinimumSize(new Dimension(60, h));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        }

        public boolean isTruncated() {
            return truncated;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            g2.setFont(font);
            g2.setColor(color);
            FontMetrics fm = g2.getFontMetrics();
            int y = fm.getAscent();
            for (String line : wrap(text, fm, getWidth() - 2)) {
                g2.drawString(line, 0, y);
                y += fm.getHeight() + 2;
            }
            g2.dispose();
        }

        private List<String> wrap(String str, FontMetrics fm, int maxW) {
            List<String> lines = new ArrayList<>();
            String[] words = str.trim().split("\\s+");
            StringBuilder cur = new StringBuilder();
            int i = 0;
            while (i < words.length && lines.size() < maxLines) {
                String cand = cur.length() == 0 ? words[i] : cur + " " + words[i];
                if (cur.length() == 0 || fm.stringWidth(cand) <= maxW) {
                    cur = new StringBuilder(cand);
                    i++;
                } else {
                    lines.add(cur.toString());
                    cur = new StringBuilder();
                }
            }
            if (cur.length() > 0 && lines.size() < maxLines) lines.add(cur.toString());

            truncated = (i < words.length);
            if (truncated && !lines.isEmpty()) {
                int last = lines.size() - 1;
                String s = lines.get(last);
                while (s.length() > 0 && fm.stringWidth(s + "...") > maxW) {
                    s = s.substring(0, s.length() - 1).trim();
                }
                lines.set(last, s + "...");
            }
            return lines;
        }
    }

    private static class WrappedTitleComponent extends JComponent {
        private final String text;
        private final Font font;
        private final Color color;
        private final boolean centered;
        private final int fixedHeight;

        public WrappedTitleComponent(String text, Font font, Color color, boolean centered, int prefWidth, int fixedHeight) {
            this.text = text;
            this.font = font;
            this.color = color;
            this.centered = centered;
            this.fixedHeight = fixedHeight;
            setOpaque(false);
            setPreferredSize(new Dimension(prefWidth, fixedHeight));
            setMinimumSize(new Dimension(80, fixedHeight));
            setMaximumSize(new Dimension(centered ? prefWidth : Integer.MAX_VALUE, fixedHeight));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            g2.setFont(font);
            g2.setColor(color);

            FontMetrics fm = g2.getFontMetrics();
            int availW = Math.max(40, getWidth() - 4);
            List<String> lines = wrapTwoLines(text, fm, availW);

            int lineHeight = fm.getHeight();
            int totalTextH = lines.size() * lineHeight;
            int startY = centered
                    ? (fixedHeight - totalTextH) / 2 + fm.getAscent()
                    : fm.getAscent();

            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                int lineW = fm.stringWidth(line);
                int x = centered ? Math.max(0, (getWidth() - lineW) / 2) : 0;
                g2.drawString(line, x, startY + (i * lineHeight));
            }

            g2.dispose();
        }

        private List<String> wrapTwoLines(String str, FontMetrics fm, int maxW) {
            List<String> result = new ArrayList<>();
            if (fm.stringWidth(str) <= maxW) {
                result.add(str);
                return result;
            }

            String[] words = str.split("\\s+");
            StringBuilder line1 = new StringBuilder();
            int idx = 0;

            while (idx < words.length) {
                String cand = line1.length() == 0 ? words[idx] : line1 + " " + words[idx];
                if (fm.stringWidth(cand) <= maxW) {
                    line1 = new StringBuilder(cand);
                    idx++;
                } else {
                    if (line1.length() == 0) {
                        line1 = new StringBuilder(ellipsize(words[idx], fm, maxW));
                        idx++;
                    }
                    break;
                }
            }
            result.add(line1.toString());

            if (idx < words.length) {
                StringBuilder line2 = new StringBuilder();
                while (idx < words.length) {
                    String cand = line2.length() == 0 ? words[idx] : line2 + " " + words[idx];
                    if (fm.stringWidth(cand) <= maxW) {
                        line2 = new StringBuilder(cand);
                        idx++;
                    } else {
                        String rem = line2.length() == 0 ? words[idx] : line2 + " " + words[idx];
                        result.add(ellipsize(rem, fm, maxW));
                        return result;
                    }
                }
                result.add(line2.toString());
            }
            return result;
        }

        private String ellipsize(String s, FontMetrics fm, int maxW) {
            String dots = "...";
            if (fm.stringWidth(s) <= maxW) return s;
            int end = s.length();
            while (end > 0 && fm.stringWidth(s.substring(0, end) + dots) > maxW) {
                end--;
            }
            return s.substring(0, Math.max(0, end)).trim() + dots;
        }
    }

    // SECTION DIVIDERS & HEADERS
    private JSeparator createSectionDivider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(UITheme.BORDER_SUBTLE);
        sep.setBackground(UITheme.BORDER_SUBTLE);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setAlignmentX(Component.LEFT_ALIGNMENT);
        return sep;
    }

    private JPanel createMovieSectionTitle(String mainTitle, String subTitle, Color barStart, Color barEnd) {
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        titlePanel.setOpaque(false);
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel gradientBar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(0, 0, barStart, 0, getHeight(), barEnd);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
            }
        };
        gradientBar.setOpaque(false);
        gradientBar.setPreferredSize(new Dimension(5, 28));

        JLabel mainLabel = new JLabel(mainTitle);
        mainLabel.setFont(UITheme.fontDisplay(21));
        mainLabel.setForeground(TEXT_WHITE);

        titlePanel.add(gradientBar);
        titlePanel.add(mainLabel);

        if (subTitle != null && !subTitle.isEmpty()) {
            JLabel subLabel = new JLabel("•  " + subTitle);
            subLabel.setFont(UITheme.fontRegular(13));
            subLabel.setForeground(TEXT_MUTED);
            titlePanel.add(subLabel);
        }

        return titlePanel;
    }

    private JPanel createFestivePromoHeader() {
        JPanel banner = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(115, 20, 48),
                        getWidth(), 0, new Color(16, 78, 60)
                );
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.setColor(new Color(225, 29, 72, 180));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(12, 18, 12, 18));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 56));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        left.setOpaque(false);
        JLabel snowIcon = new JLabel(new UITheme.VectorIcon(UITheme.VectorIcon.Type.SNOWFLAKE, 22, ACCENT_GOLD));
        JLabel title = new JLabel("Christmas Holiday Promos");
        title.setFont(UITheme.fontDisplay(21));
        title.setForeground(TEXT_WHITE);
        left.add(snowIcon);
        left.add(title);

        UITheme.PillLabel promoTag = new UITheme.PillLabel(
                "LIMITED HOLIDAY RATES • PHP 102.00",
                new Color(16, 185, 129, 75),
                new Color(167, 243, 208),
                UITheme.PROMO_GREEN
        );

        banner.add(left, BorderLayout.WEST);
        banner.add(promoTag, BorderLayout.EAST);
        return banner;
    }

    private JPanel createMovieGridSection(List<MovieGroup> groups, boolean isPromo, boolean animateCards) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        int idx = 0;
        for (MovieGroup group : groups) {
            MovieCard card = new MovieCard(group, isPromo);
            if (animateCards) {
                card.playEntrance(idx * 55, 16, 280);
            }
            grid.add(card);
            idx++;
        }
        return grid;
    }

    // 4. PREMIUM EDITORIAL MOVIE CARD WITH HOVER POSTER ZOOM & GLASS SHEEN SWEEP
    private class MovieCard extends UITheme.RoundedPanel {
        private float cardAlpha = 1.0f;
        private int cardOffsetY = 0;
        private float hoverProgress = 0f;
        private Timer hoverAnimTimer;

        public MovieCard(MovieGroup group, boolean isPromo) {
            super(
                    20,
                    isPromo ? UITheme.PROMO_CARD_BG : CARD_BG,
                    isPromo ? new Color(190, 45, 75) : UITheme.BORDER_SUBTLE,
                    true
            );
            enableHoverEffect(isPromo ? UITheme.PROMO_GREEN : ACCENT_LIGHT);
            setLayout(new BorderLayout(14, 0));
            setBorder(new EmptyBorder(14, 14, 14, 14));
            setPreferredSize(new Dimension(0, 348));

            String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
            int cinemaNo = (Math.abs(group.getId()) % 4) + 1;
            LocalDate[] range = parseMovieDateRange(group.getDateRange());
            int seatsLeft = getAvailableSeatCount(group.getTitle(), range[0], group.getTime());

            JLayeredPane posterLayer = new JLayeredPane();
            posterLayer.setPreferredSize(new Dimension(156, 260));
            posterLayer.setMinimumSize(new Dimension(156, 260));
            posterLayer.setCursor(new Cursor(Cursor.HAND_CURSOR));

            ImageIcon posterIcon = loadPoster(group.getPosterPath(), 156, 260, 16);
            Image posterImg = posterIcon != null ? posterIcon.getImage() : null;

            JComponent posterView;
            if (posterImg != null) {
                posterView = new JComponent() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        UITheme.enableAntiAliasing(g2);
                        int w = getWidth();
                        int h = getHeight();
                        g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 16, 16));
                        double zoom = 1.0 + (0.055 * hoverProgress);
                        int zw = (int) Math.round(w * zoom);
                        int zh = (int) Math.round(h * zoom);
                        int zx = (w - zw) / 2;
                        int zy = (h - zh) / 2;
                        g2.drawImage(posterImg, zx, zy, zw, zh, null);
                        g2.dispose();
                    }
                };
            } else {
                JLabel fallback = new JLabel("<html><div style='width:140px; text-align:center;'><b>SINE-SERVE</b><br><br>" + cleanTitle + "</div></html>", SwingConstants.CENTER);
                fallback.setOpaque(true);
                fallback.setBackground(PANEL_BG);
                fallback.setForeground(TEXT_MUTED);
                posterView = fallback;
            }
            posterView.setBounds(0, 0, 156, 260);
            posterLayer.add(posterView, JLayeredPane.DEFAULT_LAYER);

            UITheme.PillLabel cinemaBadge = new UITheme.PillLabel(
                    "CINEMA " + cinemaNo,
                    new Color(22, 16, 48, 225),
                    ACCENT_GLOW,
                    ACCENT_LIGHT
            );
            cinemaBadge.setFont(UITheme.fontBold(9));
            cinemaBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
            cinemaBadge.setBounds(8, 8, 72, 19);
            posterLayer.add(cinemaBadge, JLayeredPane.PALETTE_LAYER);

            if (isPromo) {
                UITheme.DiagonalRibbon ribbon = new UITheme.DiagonalRibbon("PROMO");
                ribbon.setBounds(68, 0, 88, 88);
                posterLayer.add(ribbon, JLayeredPane.PALETTE_LAYER);
            }

            posterLayer.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (isGuest) {
                        showMovieQuickPreviewDialog(group, isPromo);
                    } else {
                        startBookingFlow(group, null, 1, false, "Cash / OTC");
                    }
                }
            });

            // Smooth Hover Zoom & Glass Sheen Sweep listener across the card
            MouseAdapter cardHoverTrigger = new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    animateHover(true);
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), MovieCard.this);
                    if (!MovieCard.this.contains(p)) {
                        animateHover(false);
                    }
                }
            };
            addMouseListener(cardHoverTrigger);
            posterLayer.addMouseListener(cardHoverTrigger);

            JPanel infoPanel = new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setOpaque(false);

            WrappedTitleComponent titleComp = new WrappedTitleComponent(
                    cleanTitle,
                    UITheme.fontBold(16),
                    TEXT_WHITE,
                    false,
                    185,
                    44
            );
            titleComp.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel metaChipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            metaChipRow.setOpaque(false);
            metaChipRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            metaChipRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

            UITheme.PillLabel seatPill = new UITheme.PillLabel(
                    seatsLeft > 0 ? (seatsLeft + " seats left") : "Sold Out",
                    seatsLeft > 0 ? new Color(16, 185, 129, 34) : new Color(225, 29, 72, 45),
                    seatsLeft > 0 ? new Color(110, 231, 183) : new Color(252, 165, 165),
                    seatsLeft > 0 ? new Color(16, 185, 129, 140) : UITheme.PROMO_RED
            );
            seatPill.setFont(UITheme.fontBold(9));
            seatPill.setBorder(new EmptyBorder(2, 8, 2, 8));
            metaChipRow.add(seatPill);

            JLabel dateLbl = new JLabel(
                    " " + group.getDateRange(),
                    new UITheme.VectorIcon(UITheme.VectorIcon.Type.CALENDAR, 14, ACCENT_GLOW),
                    SwingConstants.LEFT
            );
            dateLbl.setFont(UITheme.fontRegular(12));
            dateLbl.setForeground(TEXT_MUTED);
            dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel timeLbl = new JLabel(
                    " " + group.getTime(),
                    new UITheme.VectorIcon(UITheme.VectorIcon.Type.CLOCK, 14, ACCENT_GLOW),
                    SwingConstants.LEFT
            );
            timeLbl.setFont(UITheme.fontRegular(12));
            timeLbl.setForeground(TEXT_MUTED);
            timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

            JPanel priceRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            priceRow.setOpaque(false);
            priceRow.setAlignmentX(Component.LEFT_ALIGNMENT);
            priceRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

            UITheme.PillLabel goldPricePill = new UITheme.PillLabel(
                    "PHP " + String.format("%.2f", group.getPrice()),
                    new Color(251, 191, 36, 45),
                    ACCENT_GOLD,
                    new Color(251, 191, 36, 150)
            );
            goldPricePill.setFont(UITheme.fontBold(14));
            priceRow.add(goldPricePill);

            if (isPromo) {
                JLabel origPrice = new JLabel("<html><strike>PHP 350.00</strike></html>");
                origPrice.setFont(UITheme.fontRegular(11));
                origPrice.setForeground(TEXT_MUTED);
                priceRow.add(origPrice);
            }

            UITheme.GradientButton actionBtn;
            if (isGuest) {
                actionBtn = UITheme.GradientButton.createGhost("Preview Details", ACCENT_LIGHT, 14);
                actionBtn.setForeground(TEXT_WHITE);
                actionBtn.setToolTipText("View full movie details & showtimes");
                actionBtn.addActionListener(e -> showMovieQuickPreviewDialog(group, isPromo));
            } else {
                actionBtn = new UITheme.GradientButton(
                        "RESERVE",
                        isPromo ? UITheme.PROMO_RED : ACCENT_PURPLE,
                        isPromo ? UITheme.PROMO_GREEN : ACCENT_PINK,
                        14
                );
                actionBtn.addActionListener(e -> startBookingFlow(group, null, 1, false, "Cash / OTC"));
            }

            actionBtn.setPreferredSize(new Dimension(180, 40));
            actionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            actionBtn.setFont(UITheme.fontBold(13));
            actionBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
            actionBtn.addMouseListener(cardHoverTrigger);

            DescriptionComponent descComp = new DescriptionComponent(
                    group.getDescription(),
                    UITheme.fontRegular(13),
                    new Color(209, 213, 219),
                    3
            );
            descComp.setCursor(new Cursor(Cursor.HAND_CURSOR));
            descComp.setAlignmentX(Component.LEFT_ALIGNMENT);
            attachSynopsisPopover(descComp, group);

            infoPanel.add(titleComp);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            infoPanel.add(metaChipRow);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            infoPanel.add(descComp);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 8)));
            infoPanel.add(dateLbl);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            infoPanel.add(timeLbl);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPanel.add(priceRow);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(actionBtn);

            add(posterLayer, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
        }

        private void animateHover(boolean entering) {
            if (hoverAnimTimer != null && hoverAnimTimer.isRunning()) {
                hoverAnimTimer.stop();
            }
            hoverAnimTimer = new Timer(16, e -> {
                float step = 0.12f;
                if (entering) {
                    hoverProgress = Math.min(1f, hoverProgress + step);
                    if (hoverProgress >= 1f) hoverAnimTimer.stop();
                } else {
                    hoverProgress = Math.max(0f, hoverProgress - step);
                    if (hoverProgress <= 0f) hoverAnimTimer.stop();
                }
                repaint();
            });
            hoverAnimTimer.start();
        }

        public void playEntrance(int delayMs, int slidePixels, int durationMs) {
            this.cardAlpha = 0f;
            this.cardOffsetY = slidePixels;
            repaint();

            Timer delayTimer = new Timer(Math.max(1, delayMs), e -> {
                ((Timer) e.getSource()).stop();
                final long start = System.currentTimeMillis();
                Timer animTimer = new Timer(15, null);
                animTimer.addActionListener(ev -> {
                    float raw = Math.min(1f, (System.currentTimeMillis() - start) / (float) durationMs);
                    float ease = 1f - (float) Math.pow(1f - raw, 3);
                    cardAlpha = ease;
                    cardOffsetY = Math.round(slidePixels * (1f - ease));
                    repaint();
                    if (raw >= 1f) {
                        cardAlpha = 1f;
                        cardOffsetY = 0;
                        animTimer.stop();
                    }
                });
                animTimer.start();
            });
            delayTimer.setRepeats(false);
            delayTimer.start();
        }

        @Override
        public void paint(Graphics g) {
            if (cardAlpha <= 0.01f) return;
            Graphics2D g2 = (Graphics2D) g.create();
            if (cardAlpha < 0.999f) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, cardAlpha))));
            }
            if (cardOffsetY != 0) {
                g2.translate(0, cardOffsetY);
            }
            super.paint(g2);

            // Diagonal Glass Sheen Sweep on Hover
            if (hoverProgress > 0.01f && hoverProgress < 0.99f) {
                UITheme.enableAntiAliasing(g2);
                int w = getWidth();
                int h = getHeight();
                g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, 20, 20));
                int sweepX = Math.round(-w + (w * 2.2f * hoverProgress));
                GradientPaint sheen = new GradientPaint(
                        sweepX, 0, new Color(255, 255, 255, 0),
                        sweepX + 90, h, new Color(255, 255, 255, 22)
                );
                g2.setPaint(sheen);
                g2.fillRect(0, 0, w, h);
            }
            g2.dispose();
        }
    }

    private JWindow activeSynopsisWindow;
    private Timer synopsisHoverTimer;

    private void attachSynopsisPopover(DescriptionComponent descComp, MovieGroup group) {
        descComp.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!descComp.isTruncated()) return;
                if (synopsisHoverTimer != null) synopsisHoverTimer.stop();
                synopsisHoverTimer = new Timer(220, evt -> {
                    ((Timer) evt.getSource()).stop();
                    if (descComp.isShowing() && descComp.getMousePosition() != null && isActive()) {
                        showSynopsisPopover(descComp, group);
                    }
                });
                synopsisHoverTimer.setRepeats(false);
                synopsisHoverTimer.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (synopsisHoverTimer != null) synopsisHoverTimer.stop();
                hideSynopsisPopover();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (synopsisHoverTimer != null) synopsisHoverTimer.stop();
                hideSynopsisPopover();
            }
        });
    }

    private void showSynopsisPopover(Component anchor, MovieGroup group) {
        hideSynopsisPopover();

        String clean = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        JWindow win = new JWindow(this);
        win.setFocusableWindowState(false);
        win.setBackground(new Color(0, 0, 0, 0));

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(16, new Color(26, 20, 52), ACCENT_LIGHT, true);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));

        JLabel tag = new JLabel("FULL SYNOPSIS");
        tag.setFont(UITheme.fontBold(10));
        tag.setForeground(ACCENT_GLOW);
        tag.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel("<html><div style='width:270px;'>" + clean + "</div></html>");
        title.setFont(UITheme.fontDisplay(14));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JTextArea body = new JTextArea(group.getDescription());
        body.setWrapStyleWord(true);
        body.setLineWrap(true);
        body.setEditable(false);
        body.setFocusable(false);
        body.setOpaque(false);
        body.setBorder(null);
        body.setFont(UITheme.fontRegular(12));
        body.setForeground(new Color(209, 213, 219));
        body.setSize(new Dimension(275, Short.MAX_VALUE));
        body.setPreferredSize(new Dimension(275, body.getPreferredSize().height));
        body.setAlignmentX(Component.LEFT_ALIGNMENT);

        card.add(tag);
        card.add(Box.createRigidArea(new Dimension(0, 3)));
        card.add(title);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(body);

        win.setContentPane(card);
        win.pack();

        Point loc = anchor.getLocationOnScreen();
        int x = loc.x;
        int y = loc.y + anchor.getHeight() + 6;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        if (x + win.getWidth() > screen.width - 16) x = screen.width - win.getWidth() - 16;
        if (y + win.getHeight() > screen.height - 40) y = loc.y - win.getHeight() - 6;

        win.setLocation(x, y);
        win.setVisible(true);
        UITheme.Motion.animateWindowIn(win, 8, 170);
        activeSynopsisWindow = win;

        Timer guardTimer = new Timer(60, evt -> {
            if (activeSynopsisWindow != win || !win.isVisible()) {
                ((Timer) evt.getSource()).stop();
                return;
            }
            if (!isActive() || !anchor.isShowing() || anchor.getMousePosition() == null) {
                hideSynopsisPopover();
                ((Timer) evt.getSource()).stop();
            }
        });
        guardTimer.start();
    }

    private void hideSynopsisPopover() {
        if (activeSynopsisWindow != null) {
            JWindow closing = activeSynopsisWindow;
            activeSynopsisWindow = null;
            UITheme.Motion.fadeWindowOut(closing, 120, () -> {
                closing.setVisible(false);
                closing.dispose();
            });
        }
    }

    // BOOKING, AUDITORIUM SEAT SELECTION & PERFORATED E-TICKET DIALOGS
    private LocalDate[] parseMovieDateRange(String dateRangeStr) {
        try {
            String[] parts = dateRangeStr.split("-");
            String startPart = parts[0].trim();
            String endPart = parts[1].trim();

            String yearStr = endPart.substring(endPart.lastIndexOf(",") + 1).trim();
            endPart = endPart.substring(0, endPart.lastIndexOf(",")).trim();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
            LocalDate startDate = LocalDate.parse(startPart + ", " + yearStr, fmt);
            LocalDate endDate = LocalDate.parse(endPart + ", " + yearStr, fmt);

            return new LocalDate[]{startDate, endDate};
        } catch (Exception e) {
            return new LocalDate[]{LocalDate.of(2026, 11, 4), LocalDate.of(2026, 12, 31)};
        }
    }

    private String buildSeatPlanKey(String movieTitle, LocalDate date, String showtime) {
        String cleanTitle = movieTitle.replace("[CHRISTMAS PROMO]", "").trim();
        return cleanTitle + "|" + date.toString() + "|" + showtime.trim();
    }

    private int getAvailableSeatCount(String movieTitle, LocalDate date, String showtime) {
        String key = buildSeatPlanKey(movieTitle, date, showtime);
        char[][] seats = seatPlans.computeIfAbsent(key, k -> createNewSeatPlan());
        int count = 0;
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                if (seats[r][c] != 'X') {
                    count++;
                }
            }
        }
        return count;
    }

    private int getAvailablePwdSeatCount(String movieTitle, LocalDate date, String showtime) {
        String key = buildSeatPlanKey(movieTitle, date, showtime);
        char[][] seats = seatPlans.computeIfAbsent(key, k -> createNewSeatPlan());
        int count = 0;
        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                if (seats[r][c] == 'P') {
                    count++;
                }
            }
        }
        return count;
    }

    private static class TicketInputFilter extends DocumentFilter {
        private int maxTickets;

        public TicketInputFilter(int maxTickets) {
            this.maxTickets = maxTickets;
        }

        public void setMaxTickets(int maxTickets) {
            this.maxTickets = maxTickets;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
            if (string == null) return;
            StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            sb.insert(offset, string);
            if (isValidInput(sb.toString())) {
                super.insertString(fb, offset, string, attr);
            }
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
            StringBuilder sb = new StringBuilder(fb.getDocument().getText(0, fb.getDocument().getLength()));
            sb.replace(offset, offset + length, text == null ? "" : text);
            if (isValidInput(sb.toString())) {
                super.replace(fb, offset, length, text, attrs);
            }
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
        }

        private boolean isValidInput(String text) {
            if (text.isEmpty()) return true;
            if (!text.matches("\\d+")) return false;

            int maxDigits = String.valueOf(maxTickets).length();
            if (text.length() > maxDigits) return false;

            try {
                int val = Integer.parseInt(text);
                return val <= maxTickets;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    private void startBookingFlow(MovieGroup group, LocalDate initialDate, int initialQuantity, boolean initialIsPwd, String initialPayment) {
        if (isGuest) {
            ThemedDialog.showInfo(this, "Login Required", "Guest mode is browse-only. Please log in or create an account to reserve tickets.");
            return;
        }
        hideSynopsisPopover();
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        boolean isPromo = group.getTitle().contains("[CHRISTMAS PROMO]") || group.getPrice() <= 110.0;

        JDialog dialog = new JDialog(this, "Book Tickets • " + cleanTitle, true);
        dialog.setSize(890, 630);
        dialog.setLocationRelativeTo(this);

        FadeSlidePanel root = new FadeSlidePanel(new BorderLayout(20, 0));
        root.setBackground(BG_DARK);
        root.setOpaque(true);
        root.setBorder(new EmptyBorder(20, 22, 20, 22));

        UITheme.RoundedPanel leftCard = new UITheme.RoundedPanel(20, PANEL_BG, UITheme.BORDER_SUBTLE, true);
        leftCard.setLayout(new BorderLayout(0, 14));
        leftCard.setBorder(new EmptyBorder(18, 18, 18, 18));
        leftCard.setPreferredSize(new Dimension(310, 0));

        JPanel movieTopRow = new JPanel(new BorderLayout(12, 0));
        movieTopRow.setOpaque(false);

        JLabel posterLbl;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 96, 142, 14);
        if (posterIcon != null) {
            posterLbl = new JLabel(posterIcon);
        } else {
            posterLbl = new JLabel("<html><center><b>SINE</b><br>SERVE</center></html>", SwingConstants.CENTER);
            posterLbl.setPreferredSize(new Dimension(96, 142));
            posterLbl.setOpaque(true);
            posterLbl.setBackground(CARD_BG);
            posterLbl.setForeground(TEXT_MUTED);
        }

        JPanel movieMetaCol = new JPanel();
        movieMetaCol.setLayout(new BoxLayout(movieMetaCol, BoxLayout.Y_AXIS));
        movieMetaCol.setOpaque(false);

        UITheme.PillLabel formatBadge = new UITheme.PillLabel(
                isPromo ? "CHRISTMAS PROMO" : "STANDARD 2D",
                isPromo ? new Color(225, 29, 72, 55) : new Color(124, 58, 237, 55),
                isPromo ? new Color(252, 165, 165) : ACCENT_GLOW,
                isPromo ? UITheme.PROMO_RED : ACCENT_PURPLE
        );
        formatBadge.setFont(UITheme.fontBold(10));
        formatBadge.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel titleLbl = new JLabel("<html><div style='width:150px;'>" + cleanTitle + "</div></html>");
        titleLbl.setFont(UITheme.fontDisplay(16));
        titleLbl.setForeground(TEXT_WHITE);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateRangeLbl = new JLabel(" " + group.getDateRange(), new UITheme.VectorIcon(UITheme.VectorIcon.Type.CALENDAR, 12, TEXT_MUTED), SwingConstants.LEFT);
        dateRangeLbl.setFont(UITheme.fontRegular(11));
        dateRangeLbl.setForeground(TEXT_MUTED);
        dateRangeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLbl = new JLabel(" " + group.getTime(), new UITheme.VectorIcon(UITheme.VectorIcon.Type.CLOCK, 12, ACCENT_GLOW), SwingConstants.LEFT);
        timeLbl.setFont(UITheme.fontRegular(12));
        timeLbl.setForeground(TEXT_MUTED);
        timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel basePriceLbl = new JLabel("PHP " + String.format("%.2f", group.getPrice()) + " / seat");
        basePriceLbl.setFont(UITheme.fontBold(12));
        basePriceLbl.setForeground(ACCENT_GOLD);
        basePriceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        movieMetaCol.add(formatBadge);
        movieMetaCol.add(Box.createRigidArea(new Dimension(0, 6)));
        movieMetaCol.add(titleLbl);
        movieMetaCol.add(Box.createRigidArea(new Dimension(0, 6)));
        movieMetaCol.add(dateRangeLbl);
        movieMetaCol.add(Box.createRigidArea(new Dimension(0, 3)));
        movieMetaCol.add(timeLbl);
        movieMetaCol.add(Box.createRigidArea(new Dimension(0, 5)));
        movieMetaCol.add(basePriceLbl);

        movieTopRow.add(posterLbl, BorderLayout.WEST);
        movieTopRow.add(movieMetaCol, BorderLayout.CENTER);

        UITheme.RoundedPanel synopsisBox = new UITheme.RoundedPanel(14, new Color(20, 15, 38), new Color(55, 44, 94), false);
        synopsisBox.setLayout(new BorderLayout(0, 6));
        synopsisBox.setBorder(new EmptyBorder(12, 14, 12, 14));

        JLabel synHeader = new JLabel("SYNOPSIS");
        synHeader.setFont(UITheme.fontBold(10));
        synHeader.setForeground(ACCENT_GLOW);

        JTextArea synopsisArea = new JTextArea(group.getDescription());
        synopsisArea.setWrapStyleWord(true);
        synopsisArea.setLineWrap(true);
        synopsisArea.setEditable(false);
        synopsisArea.setFocusable(false);
        synopsisArea.setOpaque(false);
        synopsisArea.setBorder(null);
        synopsisArea.setFont(UITheme.fontRegular(13));
        synopsisArea.setForeground(TEXT_WHITE);

        synopsisBox.add(synHeader, BorderLayout.NORTH);
        synopsisBox.add(synopsisArea, BorderLayout.CENTER);

        UITheme.RoundedPanel summaryCard = new UITheme.RoundedPanel(14, new Color(18, 13, 36), UITheme.BORDER_SUBTLE, false);
        summaryCard.setLayout(new BoxLayout(summaryCard, BoxLayout.Y_AXIS));
        summaryCard.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel sumHeaderRow = new JPanel(new BorderLayout());
        sumHeaderRow.setOpaque(false);
        sumHeaderRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));
        JLabel sumHeader = new JLabel("BOOKING SUMMARY");
        sumHeader.setFont(UITheme.fontBold(10));
        sumHeader.setForeground(ACCENT_GLOW);
        sumHeaderRow.add(sumHeader, BorderLayout.WEST);

        JLabel sumDateVal = new JLabel("-");
        sumDateVal.setFont(UITheme.fontBold(12));
        sumDateVal.setForeground(TEXT_WHITE);

        JLabel sumQtyVal = new JLabel("1 Ticket");
        sumQtyVal.setFont(UITheme.fontRegular(12));
        sumQtyVal.setForeground(TEXT_MUTED);

        JLabel sumDiscountVal = new JLabel("None");
        sumDiscountVal.setFont(UITheme.fontRegular(12));
        sumDiscountVal.setForeground(TEXT_MUTED);

        JLabel sumFeeVal = new JLabel("PHP 10.00");
        sumFeeVal.setFont(UITheme.fontRegular(12));
        sumFeeVal.setForeground(TEXT_MUTED);

        JLabel sumTotalVal = new JLabel("PHP 0.00");
        sumTotalVal.setFont(UITheme.fontDisplay(18));
        sumTotalVal.setForeground(ACCENT_GOLD);

        summaryCard.add(sumHeaderRow);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 10)));
        summaryCard.add(createSummaryLine("Show Date:", sumDateVal));
        summaryCard.add(Box.createRigidArea(new Dimension(0, 6)));
        summaryCard.add(createSummaryLine("Quantity:", sumQtyVal));
        summaryCard.add(Box.createRigidArea(new Dimension(0, 6)));
        summaryCard.add(createSummaryLine("Discount:", sumDiscountVal));
        summaryCard.add(Box.createRigidArea(new Dimension(0, 6)));
        summaryCard.add(createSummaryLine("Booking Fee:", sumFeeVal));
        summaryCard.add(Box.createRigidArea(new Dimension(0, 10)));

        JSeparator sumSep = new JSeparator();
        sumSep.setForeground(UITheme.BORDER_SUBTLE);
        sumSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        summaryCard.add(sumSep);
        summaryCard.add(Box.createRigidArea(new Dimension(0, 10)));
        summaryCard.add(createSummaryLine("Est. Total:", sumTotalVal));

        leftCard.add(movieTopRow, BorderLayout.NORTH);
        leftCard.add(synopsisBox, BorderLayout.CENTER);
        leftCard.add(summaryCard, BorderLayout.SOUTH);

        JPanel rightCol = new JPanel();
        rightCol.setLayout(new BoxLayout(rightCol, BoxLayout.Y_AXIS));
        rightCol.setOpaque(false);

        JPanel step1Header = new JPanel(new BorderLayout());
        step1Header.setOpaque(false);
        step1Header.setAlignmentX(Component.LEFT_ALIGNMENT);
        step1Header.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JLabel calTitle = new JLabel("1. Choose Show Date");
        calTitle.setFont(UITheme.fontBold(15));
        calTitle.setForeground(TEXT_WHITE);

        UITheme.PillLabel statusPill = new UITheme.PillLabel(
                "Available Seats: 87 / 90",
                new Color(124, 58, 237, 45),
                ACCENT_GLOW,
                ACCENT_PURPLE
        );
        statusPill.setFont(UITheme.fontBold(11));

        step1Header.add(calTitle, BorderLayout.WEST);
        step1Header.add(statusPill, BorderLayout.EAST);

        LocalDate[] range = parseMovieDateRange(group.getDateRange());
        final LocalDate[] chosenDateHolder = {initialDate != null ? initialDate : range[0]};

        JTextField ticketTextField = new JTextField(String.valueOf(initialQuantity));
        ticketTextField.setPreferredSize(new Dimension(64, 36));
        ticketTextField.setMaximumSize(new Dimension(64, 36));
        ticketTextField.setBackground(UITheme.INPUT_BG);
        ticketTextField.setForeground(TEXT_WHITE);
        ticketTextField.setCaretColor(ACCENT_GLOW);
        ticketTextField.setFont(UITheme.fontBold(15));
        ticketTextField.setHorizontalAlignment(JTextField.CENTER);
        ticketTextField.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE, 1));

        final TicketInputFilter ticketFilter = new TicketInputFilter(90);
        ((AbstractDocument) ticketTextField.getDocument()).setDocumentFilter(ticketFilter);

        JButton minusBtn = new UITheme.SoftButton("−", 10);
        minusBtn.setPreferredSize(new Dimension(36, 36));
        minusBtn.setBackground(CARD_BG);
        minusBtn.setForeground(TEXT_WHITE);
        minusBtn.setFont(UITheme.fontBold(16));

        JButton plusBtn = new UITheme.SoftButton("+", 10);
        plusBtn.setPreferredSize(new Dimension(36, 36));
        plusBtn.setBackground(CARD_BG);
        plusBtn.setForeground(TEXT_WHITE);
        plusBtn.setFont(UITheme.fontBold(16));

        JPanel stepperRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        stepperRow.setOpaque(false);
        stepperRow.setPreferredSize(new Dimension(170, 42));
        stepperRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        stepperRow.add(minusBtn);
        stepperRow.add(ticketTextField);
        stepperRow.add(plusBtn);

        UITheme.RoundedPanel pwdCard = new UITheme.RoundedPanel(12, PANEL_BG, UITheme.BORDER_SUBTLE, false);
        pwdCard.setLayout(new BorderLayout(8, 0));
        pwdCard.setBorder(new EmptyBorder(10, 14, 10, 14));

        JPanel pwdTextCol = new JPanel();
        pwdTextCol.setLayout(new BoxLayout(pwdTextCol, BoxLayout.Y_AXIS));
        pwdTextCol.setOpaque(false);
        JLabel pwdMain = new JLabel("PWD / Senior Citizen");
        pwdMain.setFont(UITheme.fontBold(12));
        pwdMain.setForeground(TEXT_WHITE);
        JLabel pwdSub = new JLabel("20% OFF + Priority Seats");
        pwdSub.setFont(UITheme.fontRegular(11));
        pwdSub.setForeground(new Color(110, 231, 183));
        pwdTextCol.add(pwdMain);
        pwdTextCol.add(Box.createRigidArea(new Dimension(0, 2)));
        pwdTextCol.add(pwdSub);

        UITheme.ToggleSwitch pwdToggle = new UITheme.ToggleSwitch();
        pwdToggle.setSelected(initialIsPwd);
        JPanel switchCenterWrap = new JPanel(new GridBagLayout());
        switchCenterWrap.setOpaque(false);
        switchCenterWrap.add(pwdToggle);

        pwdCard.add(pwdTextCol, BorderLayout.CENTER);
        pwdCard.add(switchCenterWrap, BorderLayout.EAST);

        JPanel qtyAndPwdRow = new JPanel(new GridLayout(1, 2, 14, 0));
        qtyAndPwdRow.setOpaque(false);
        qtyAndPwdRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        qtyAndPwdRow.setPreferredSize(new Dimension(0, 70));
        qtyAndPwdRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 70));

        JPanel qtyBox = new JPanel();
        qtyBox.setLayout(new BoxLayout(qtyBox, BoxLayout.Y_AXIS));
        qtyBox.setOpaque(false);
        JLabel qtyLbl = new JLabel("2. Number of Tickets");
        qtyLbl.setFont(UITheme.fontBold(13));
        qtyLbl.setForeground(TEXT_WHITE);
        qtyLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        stepperRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        qtyBox.add(qtyLbl);
        qtyBox.add(Box.createRigidArea(new Dimension(0, 6)));
        qtyBox.add(stepperRow);

        qtyAndPwdRow.add(qtyBox);
        qtyAndPwdRow.add(pwdCard);

        JLabel payTitle = new JLabel("3. Select Payment Method");
        payTitle.setFont(UITheme.fontBold(13));
        payTitle.setForeground(TEXT_WHITE);
        payTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[] paymentOptions = {"Cash / OTC", "GCash", "Maya", "Bank Account"};
        String[] paymentFullNames = {"Cash / OTC", "GCash", "Maya", "Bank Account"};
        final String[] selectedPaymentHolder = {initialPayment != null ? initialPayment : paymentFullNames[0]};
        final Runnable[] refreshSummaryHolder = {() -> {}};

        JPanel paymentGrid = new JPanel(new GridLayout(1, 4, 8, 0));
        paymentGrid.setOpaque(false);
        paymentGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentGrid.setPreferredSize(new Dimension(0, 40));
        paymentGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        List<UITheme.PaymentOptionButton> payButtons = new ArrayList<>();
        for (int i = 0; i < paymentOptions.length; i++) {
            String shortLabel = paymentOptions[i];
            String fullValue = paymentFullNames[i];
            UITheme.PaymentOptionButton pBtn = new UITheme.PaymentOptionButton(shortLabel);
            pBtn.setSelectedOption(selectedPaymentHolder[0].equals(fullValue));
            pBtn.addActionListener(e -> {
                selectedPaymentHolder[0] = fullValue;
                for (int j = 0; j < payButtons.size(); j++) {
                    payButtons.get(j).setSelectedOption(paymentFullNames[j].equals(fullValue));
                }
                refreshSummaryHolder[0].run();
            });
            payButtons.add(pBtn);
            paymentGrid.add(pBtn);
        }

        JPanel ctaRow = new JPanel(new BorderLayout(12, 0));
        ctaRow.setOpaque(false);
        ctaRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        ctaRow.setPreferredSize(new Dimension(0, 44));
        ctaRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        UITheme.GradientButton cancelBtn = UITheme.GradientButton.createGhost("Cancel", UITheme.BORDER_SUBTLE, 14);
        cancelBtn.setPreferredSize(new Dimension(110, 44));
        cancelBtn.addActionListener(e -> dialog.dispose());

        UITheme.GradientButton nextBtn = new UITheme.GradientButton("Proceed to Seat Selection  →", ACCENT_PURPLE, ACCENT_PINK, 14);
        nextBtn.setPreferredSize(new Dimension(0, 44));
        nextBtn.setFont(UITheme.fontBold(14));

        ctaRow.add(cancelBtn, BorderLayout.WEST);
        ctaRow.add(nextBtn, BorderLayout.CENTER);

        java.util.function.Supplier<Integer> getValidTickets = () -> {
            String text = ticketTextField.getText().trim();
            int currentMax = ticketFilter.maxTickets;
            int parsedVal = 1;

            if (!text.isEmpty()) {
                try {
                    parsedVal = Integer.parseInt(text);
                } catch (NumberFormatException ignored) {}
            }

            if (parsedVal < 1) parsedVal = 1;
            if (parsedVal > currentMax) parsedVal = currentMax;

            ticketTextField.setText(String.valueOf(parsedVal));
            return parsedVal;
        };

        DateTimeFormatter prettyDateFmt = DateTimeFormatter.ofPattern("MMM d, yyyy (EEE)");

        Runnable updateAvailabilityAndSummary = () -> {
            LocalDate selectedDate = chosenDateHolder[0];
            int totalAvailable = getAvailableSeatCount(group.getTitle(), selectedDate, group.getTime());
            int unreservedPwd = getAvailablePwdSeatCount(group.getTitle(), selectedDate, group.getTime());
            boolean isPwd = pwdToggle.isSelected();

            int allowedMax = isPwd ? totalAvailable : (totalAvailable - unreservedPwd);

            if (allowedMax <= 0) {
                statusPill.setText("Fully Booked");
                statusPill.setForeground(ACCENT_PINK);
                ticketTextField.setEnabled(false);
                minusBtn.setEnabled(false);
                plusBtn.setEnabled(false);
                nextBtn.setEnabled(false);
            } else {
                statusPill.setText("Available Seats: " + allowedMax + " / 90");
                statusPill.setForeground(ACCENT_GLOW);
                ticketTextField.setEnabled(true);
                minusBtn.setEnabled(true);
                plusBtn.setEnabled(true);
                nextBtn.setEnabled(true);

                ticketFilter.setMaxTickets(allowedMax);

                int currentVal = getValidTickets.get();
                if (currentVal > allowedMax) {
                    ticketTextField.setText(String.valueOf(allowedMax));
                }
            }

            int qty = Math.max(1, getValidTickets.get());
            BillingCalculator previewBill = new BillingCalculator(group.getPrice(), qty, isPwd, selectedPaymentHolder[0]);
            sumDateVal.setText(selectedDate.format(prettyDateFmt));
            sumQtyVal.setText(qty + (qty == 1 ? " Ticket" : " Tickets"));
            sumDiscountVal.setText(isPwd ? "20% OFF (-PHP " + String.format("%.2f", previewBill.getDiscountAmount()) + ")" : "None");
            sumDiscountVal.setForeground(isPwd ? new Color(110, 231, 183) : TEXT_MUTED);
            sumFeeVal.setText("PHP " + String.format("%.2f", previewBill.getOnlineFee()));
            sumTotalVal.setText("PHP " + String.format("%.2f", previewBill.getFinalTotal()));
        };

        minusBtn.addActionListener(e -> {
            int cur = getValidTickets.get();
            if (cur > 1) ticketTextField.setText(String.valueOf(cur - 1));
            updateAvailabilityAndSummary.run();
        });

        plusBtn.addActionListener(e -> {
            int cur = getValidTickets.get();
            if (cur < ticketFilter.maxTickets) ticketTextField.setText(String.valueOf(cur + 1));
            updateAvailabilityAndSummary.run();
        });

        ticketTextField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(ticketTextField::selectAll); }
            @Override public void focusLost(FocusEvent e) { updateAvailabilityAndSummary.run(); }
        });
        ticketTextField.addActionListener(e -> updateAvailabilityAndSummary.run());
        pwdToggle.addActionListener(e -> updateAvailabilityAndSummary.run());

        JPanel calendarPanel = createCustomCalendarPicker(chosenDateHolder, range[0], range[1], updateAvailabilityAndSummary);
        calendarPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        refreshSummaryHolder[0] = updateAvailabilityAndSummary;
        updateAvailabilityAndSummary.run();

        nextBtn.addActionListener(e -> {
            int quantity = getValidTickets.get();
            LocalDate chosenDate = chosenDateHolder[0];
            String dateInput = chosenDate.toString();
            boolean hasDiscount = pwdToggle.isSelected();
            String paymentMethod = selectedPaymentHolder[0];

            try {
                MovieSchedule ticket = cinemaManager.getMovieTicket(dateInput, group.getId());
                LocalDate validDate = ticket.getShowDate();
                BillingCalculator billing = new BillingCalculator(ticket.getPrice(), quantity, hasDiscount, paymentMethod);

                dialog.dispose();
                showSeatPlanDialog(group.getTitle(), validDate, ticket.getShowTime(), ticket, billing, quantity, group, hasDiscount, paymentMethod);

            } catch (InvalidDateException ex) {
                ThemedDialog.showError(dialog, "Invalid Date", ex.getMessage());
            } catch (ScheduleNotFoundException ex) {
                ThemedDialog.showError(dialog, "Schedule Not Found", ex.getMessage());
            } catch (Exception ex) {
                ThemedDialog.showError(dialog, "Schedule Error", "The selected date is outside this movie's schedule range!");
            }
        });

        rightCol.add(step1Header);
        rightCol.add(Box.createRigidArea(new Dimension(0, 8)));
        rightCol.add(calendarPanel);
        rightCol.add(Box.createRigidArea(new Dimension(0, 16)));
        rightCol.add(qtyAndPwdRow);
        rightCol.add(Box.createRigidArea(new Dimension(0, 18)));
        rightCol.add(payTitle);
        rightCol.add(Box.createRigidArea(new Dimension(0, 8)));
        rightCol.add(paymentGrid);
        rightCol.add(Box.createRigidArea(new Dimension(0, 22)));
        rightCol.add(ctaRow);

        root.add(leftCard, BorderLayout.WEST);
        root.add(rightCol, BorderLayout.CENTER);

        refreshFilteredMovies(false);

        dialog.setContentPane(root);
        root.playDirectionalEntrance(0, initialDate != null ? -28 : 0, initialDate != null ? 0 : 20, 260);
        dialog.setVisible(true);
    }

    private JPanel createSummaryLine(String label, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.fontRegular(12));
        lbl.setForeground(TEXT_MUTED);
        row.add(lbl, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }

    private JPanel createCustomCalendarPicker(LocalDate[] chosenDateHolder, LocalDate minDate, LocalDate maxDate, Runnable onDateSelected) {
        UITheme.RoundedPanel calContainer = new UITheme.RoundedPanel(16, PANEL_BG, UITheme.BORDER_SUBTLE, false);
        calContainer.setLayout(new BorderLayout(0, 8));
        calContainer.setBorder(new EmptyBorder(12, 14, 12, 14));
        calContainer.setPreferredSize(new Dimension(0, 326));
        calContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 326));

        final YearMonth[] currentYM = {YearMonth.from(chosenDateHolder[0])};

        JLabel monthYearHeader = new JLabel("", SwingConstants.CENTER);
        monthYearHeader.setFont(UITheme.fontBold(15));
        monthYearHeader.setForeground(TEXT_WHITE);

        JPanel gridPanel = new JPanel(new GridLayout(0, 7, 6, 6));
        gridPanel.setOpaque(false);

        UITheme.GradientButton prevBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 12);
        prevBtn.setPreferredSize(new Dimension(34, 30));
        prevBtn.setIcon(new ChevronArrowIcon(true, 11, TEXT_WHITE));

        UITheme.GradientButton nextBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 12);
        nextBtn.setPreferredSize(new Dimension(34, 30));
        nextBtn.setIcon(new ChevronArrowIcon(false, 11, TEXT_WHITE));

        Runnable rebuildGrid = new Runnable() {
            @Override
            public void run() {
                gridPanel.removeAll();
                monthYearHeader.setText(currentYM[0].getMonth().toString() + " " + currentYM[0].getYear());

                String[] days = {"SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
                for (String d : days) {
                    JLabel dLbl = new JLabel(d, SwingConstants.CENTER);
                    dLbl.setFont(UITheme.fontBold(11));
                    dLbl.setForeground(ACCENT_GLOW);
                    gridPanel.add(dLbl);
                }

                LocalDate firstOfMonth = currentYM[0].atDay(1);
                int dayOfWeekVal = firstOfMonth.getDayOfWeek().getValue() % 7;
                int daysInMonth = currentYM[0].lengthOfMonth();

                for (int i = 0; i < dayOfWeekVal; i++) {
                    gridPanel.add(new JLabel(""));
                }

                for (int day = 1; day <= daysInMonth; day++) {
                    LocalDate thisDate = currentYM[0].atDay(day);
                    boolean isValidRange = !thisDate.isBefore(minDate) && !thisDate.isAfter(maxDate);
                    boolean isSelected = thisDate.equals(chosenDateHolder[0]);

                    JButton dayBtn = new JButton(String.valueOf(day)) {
                        private boolean hovered = false;
                        {
                            setContentAreaFilled(false);
                            setBorderPainted(false);
                            setFocusPainted(false);
                            setOpaque(false);
                            setMargin(new Insets(0, 0, 0, 0));
                            setPreferredSize(new Dimension(48, 38));
                            setFont(UITheme.fontBold(13));
                            addMouseListener(new MouseAdapter() {
                                @Override public void mouseEntered(MouseEvent e) { if (isEnabled()) { hovered = true; repaint(); } }
                                @Override public void mouseExited(MouseEvent e) { hovered = false; repaint(); }
                            });
                        }

                        @Override
                        protected void paintComponent(Graphics g) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            UITheme.enableAntiAliasing(g2);
                            int w = getWidth();
                            int h = getHeight();

                            if (isSelected) {
                                GradientPaint gp = new GradientPaint(0, 0, ACCENT_PURPLE, w, h, ACCENT_PINK);
                                g2.setPaint(gp);
                                g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);
                                g2.setStroke(new BasicStroke(1.5f));
                                g2.setColor(TEXT_WHITE);
                                g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
                            } else if (isValidRange) {
                                g2.setColor(hovered ? UITheme.CARD_HOVER : CARD_BG);
                                g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);
                                g2.setColor(hovered ? ACCENT_LIGHT : UITheme.BORDER_SUBTLE);
                                g2.drawRoundRect(1, 1, w - 3, h - 3, 12, 12);
                            } else {
                                g2.setColor(new Color(20, 15, 36, 140));
                                g2.fillRoundRect(1, 1, w - 2, h - 2, 12, 12);
                            }
                            g2.dispose();
                            super.paintComponent(g);
                        }
                    };

                    if (isValidRange) {
                        dayBtn.setForeground(TEXT_WHITE);
                        dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        dayBtn.addActionListener(e -> {
                            chosenDateHolder[0] = thisDate;
                            run();
                            if (onDateSelected != null) onDateSelected.run();
                        });
                    } else {
                        dayBtn.setForeground(new Color(107, 114, 128, 120));
                        dayBtn.setEnabled(false);
                    }

                    gridPanel.add(dayBtn);
                }

                gridPanel.revalidate();
                gridPanel.repaint();
            }
        };

        prevBtn.addActionListener(e -> {
            currentYM[0] = currentYM[0].minusMonths(1);
            rebuildGrid.run();
        });

        nextBtn.addActionListener(e -> {
            currentYM[0] = currentYM[0].plusMonths(1);
            rebuildGrid.run();
        });

        rebuildGrid.run();

        JPanel topNav = new JPanel(new BorderLayout());
        topNav.setOpaque(false);
        topNav.add(prevBtn, BorderLayout.WEST);
        topNav.add(monthYearHeader, BorderLayout.CENTER);
        topNav.add(nextBtn, BorderLayout.EAST);

        calContainer.add(topNav, BorderLayout.NORTH);
        calContainer.add(gridPanel, BorderLayout.CENTER);

        return calContainer;
    }

    // REALISTIC AUDITORIUM SEAT SELECTION WITH ROW HOVER HIGHLIGHT & BREATHING PULSE
    private void showSeatPlanDialog(String movieTitle, LocalDate showDate, String showtime, MovieSchedule ticket, BillingCalculator billing, int requiredSeats, MovieGroup group, boolean isPwdSenior, String paymentMethod) {
        String cleanMovieTitle = movieTitle.replace("[CHRISTMAS PROMO]", "").trim();
        String seatWord = (requiredSeats == 1) ? "Seat" : "Seats";
        JDialog dialog = new JDialog(
                this,
                "Auditorium Seat Selection • " + cleanMovieTitle,
                true
        );
        dialog.setSize(960, 840);
        dialog.setLocationRelativeTo(this);

        FadeSlidePanel mainPanel = new FadeSlidePanel(new BorderLayout(10, 10));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setOpaque(true);
        mainPanel.setBorder(new EmptyBorder(16, 22, 16, 22));

        JPanel northContainer = new JPanel();
        northContainer.setLayout(new BoxLayout(northContainer, BoxLayout.Y_AXIS));
        northContainer.setOpaque(false);

        JPanel topInfoBar = new JPanel(new BorderLayout());
        topInfoBar.setOpaque(false);

        JLabel movieHeader = new JLabel(cleanMovieTitle);
        movieHeader.setFont(UITheme.fontDisplay(20));
        movieHeader.setForeground(TEXT_WHITE);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy");
        UITheme.PillLabel showPill = new UITheme.PillLabel(
                showDate.format(fmt) + "  •  " + showtime + "  •  " + requiredSeats + " " + seatWord,
                new Color(124, 58, 237, 55),
                ACCENT_GLOW,
                ACCENT_PURPLE
        );
        showPill.setFont(UITheme.fontBold(12));

        topInfoBar.add(movieHeader, BorderLayout.WEST);
        topInfoBar.add(showPill, BorderLayout.EAST);

        UITheme.CurvedCinemaScreen curvedScreen = new UITheme.CurvedCinemaScreen();
        curvedScreen.setAlignmentX(Component.CENTER_ALIGNMENT);

        northContainer.add(topInfoBar);
        northContainer.add(Box.createRigidArea(new Dimension(0, 8)));
        northContainer.add(curvedScreen);
        mainPanel.add(northContainer, BorderLayout.NORTH);

        String seatKey = buildSeatPlanKey(movieTitle, showDate, showtime);
        char[][] seats = seatPlans.computeIfAbsent(seatKey, k -> createNewSeatPlan());
        List<Point> currentSelections = new ArrayList<>();
        char[] rowLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};
        final int[] hoveredRow = {-1};
        final float[] pulsePhase = {0f};

        UITheme.RoundedPanel auditoriumCard = new UITheme.RoundedPanel(20, PANEL_BG, UITheme.BORDER_SUBTLE, false) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (hoveredRow[0] >= 0 && hoveredRow[0] < 10) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    UITheme.enableAntiAliasing(g2);
                    int innerH = getHeight() - 28;
                    int rowH = innerH / 10;
                    int rowY = 14 + hoveredRow[0] * rowH;
                    g2.setColor(new Color(192, 132, 252, 22));
                    g2.fillRoundRect(16, rowY, getWidth() - 32, rowH, 10, 10);
                    g2.dispose();
                }
            }
        };
        auditoriumCard.setLayout(new GridBagLayout());
        auditoriumCard.setBorder(new EmptyBorder(14, 18, 14, 18));

        Timer pulseTimer = new Timer(50, e -> {
            if (!currentSelections.isEmpty()) {
                pulsePhase[0] += 0.16f;
                auditoriumCard.repaint();
            }
        });
        pulseTimer.start();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(3, 5, 3, 5);
        gbc.fill = GridBagConstraints.NONE;

        JLabel selectionSummaryLbl = new JLabel("Selected Seats (0 / " + requiredSeats + "): None");
        selectionSummaryLbl.setFont(UITheme.fontBold(13));
        selectionSummaryLbl.setForeground(ACCENT_GLOW);

        Runnable updateSummaryLabel = () -> {
            if (currentSelections.isEmpty()) {
                selectionSummaryLbl.setText("Selected Seats (0 / " + requiredSeats + "): None");
            } else {
                List<Point> sorted = new ArrayList<>(currentSelections);
                sorted.sort(Comparator.comparingInt((Point p) -> p.x).thenComparingInt(p -> p.y));
                List<String> names = new ArrayList<>();
                for (Point p : sorted) {
                    names.add(rowLetters[p.x] + String.valueOf(p.y + 1));
                }
                selectionSummaryLbl.setText("Selected Seats (" + currentSelections.size() + " / " + requiredSeats + "):  " + String.join(", ", names));
            }
        };

JLabel[] leftRowLabels = new JLabel[10];
JLabel[] rightRowLabels = new JLabel[10];


        for (int r = 0; r < 10; r++) {
            final int rowIdx = r;
            gbc.gridy = r;

            gbc.gridx = 0;
            JLabel leftRowLbl = new JLabel(String.valueOf(rowLetters[r]), SwingConstants.CENTER);
            leftRowLbl.setPreferredSize(new Dimension(22, 40));
            leftRowLbl.setFont(UITheme.fontBold(12));
            leftRowLbl.setForeground(TEXT_MUTED);
            leftRowLabels[r] = leftRowLbl;
            auditoriumCard.add(leftRowLbl, gbc);

            for (int c = 0; c < 9; c++) {
                int gridCol = (c < 4) ? (c + 1) : (c + 2);
                gbc.gridx = gridCol;

                char status = seats[r][c];
                String seatLabelText = rowLetters[r] + String.valueOf(c + 1);

                UITheme.TheaterSeatButton.SeatState initialState;
                if (status == 'X') {
                    initialState = UITheme.TheaterSeatButton.SeatState.RESERVED;
                } else if (status == 'P') {
                    initialState = isPwdSenior
                            ? UITheme.TheaterSeatButton.SeatState.PWD
                            : UITheme.TheaterSeatButton.SeatState.PWD_LOCKED;
                } else {
                    initialState = UITheme.TheaterSeatButton.SeatState.AVAILABLE;
                }

                String displayCode = (status == 'X') ? "TAKEN" : (status == 'P' ? seatLabelText + "P" : seatLabelText);
                int finalR = r;
                int finalC = c;
                Point pt = new Point(finalR, finalC);

                UITheme.TheaterSeatButton seatBtn = new UITheme.TheaterSeatButton(displayCode, initialState) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        if (currentSelections.contains(pt)) {
                            Graphics2D g2 = (Graphics2D) g.create();
                            UITheme.enableAntiAliasing(g2);
                            int glowAlpha = 95 + (int) Math.round(70 * Math.sin(pulsePhase[0]));
                            g2.setColor(new Color(236, 72, 153, Math.max(40, Math.min(200, glowAlpha))));
                            g2.setStroke(new BasicStroke(2f));
                            g2.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 10, 10);
                            g2.dispose();
                        }
                    }
                };

                seatBtn.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hoveredRow[0] = rowIdx;
                        leftRowLabels[rowIdx].setForeground(ACCENT_GOLD);
                        rightRowLabels[rowIdx].setForeground(ACCENT_GOLD);
                        auditoriumCard.repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        if (hoveredRow[0] == rowIdx) {
                            hoveredRow[0] = -1;
                        }
                        leftRowLabels[rowIdx].setForeground(TEXT_MUTED);
                        rightRowLabels[rowIdx].setForeground(TEXT_MUTED);
                        auditoriumCard.repaint();
                    }
                });

                if (status == 'X') {
                    seatBtn.setEnabled(false);
                    seatBtn.setToolTipText("Seat " + seatLabelText + " is already reserved");
                } else if (status == 'P' && !isPwdSenior) {
                    seatBtn.setEnabled(false);
                    seatBtn.setToolTipText("Seat " + seatLabelText + " is reserved for PWD / Senior Citizens only");
                } else if (status == 'P') {
                    seatBtn.setToolTipText("PWD / Senior Citizen Priority Seat " + seatLabelText);
                } else {
                    seatBtn.setToolTipText("Standard Seat " + seatLabelText);
                }

                seatBtn.addActionListener(e -> {
                    if (currentSelections.contains(pt)) {
                        currentSelections.remove(pt);
                        seatBtn.setSeatState(seats[finalR][finalC] == 'P'
                                ? UITheme.TheaterSeatButton.SeatState.PWD
                                : UITheme.TheaterSeatButton.SeatState.AVAILABLE);
                        updateSummaryLabel.run();
                        return;
                    }

                    if (currentSelections.size() >= requiredSeats) {
                        String limitSeatWord = (requiredSeats == 1) ? "seat" : "seats";
                        ThemedDialog.showInfo(dialog, "Limit Reached", "You can only select up to " + requiredSeats + " " + limitSeatWord + ".");
                        return;
                    }

                    currentSelections.add(pt);
                    seatBtn.setSeatState(UITheme.TheaterSeatButton.SeatState.SELECTED);
                    updateSummaryLabel.run();
                });

                auditoriumCard.add(seatBtn, gbc);
            }

            if (r == 0) {
                gbc.gridx = 5;
                gbc.gridheight = 10;
                JLabel aisleLbl = new JLabel("<html><div style='text-align:center; color:#6B7280; font-size:9px;'>A<br>I<br>S<br>L<br>E</div></html>", SwingConstants.CENTER);
                aisleLbl.setPreferredSize(new Dimension(34, 400));
                auditoriumCard.add(aisleLbl, gbc);
                gbc.gridheight = 1;
            }

            gbc.gridx = 11;
            JLabel rightRowLbl = new JLabel(String.valueOf(rowLetters[r]), SwingConstants.CENTER);
            rightRowLbl.setPreferredSize(new Dimension(22, 40));
            rightRowLbl.setFont(UITheme.fontBold(12));
            rightRowLbl.setForeground(TEXT_MUTED);
            rightRowLabels[r] = rightRowLbl;
            auditoriumCard.add(rightRowLbl, gbc);
        }

        mainPanel.add(auditoriumCard, BorderLayout.CENTER);

        JPanel southContainer = new JPanel();
        southContainer.setLayout(new BoxLayout(southContainer, BoxLayout.Y_AXIS));
        southContainer.setOpaque(false);

        JPanel statusStrip = new JPanel(new BorderLayout());
        statusStrip.setOpaque(false);
        statusStrip.setBorder(new EmptyBorder(0, 4, 8, 4));

        JPanel legendRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        legendRow.setOpaque(false);
        legendRow.add(createSeatLegendItem(new Color(46, 38, 92), "Available"));
        legendRow.add(createSeatLegendItem(ACCENT_LIGHT, "Selected"));
        legendRow.add(createSeatLegendItem(new Color(115, 28, 38), "Reserved"));
        legendRow.add(createSeatLegendItem(new Color(180, 120, 8), "PWD / Senior"));

        statusStrip.add(legendRow, BorderLayout.WEST);
        statusStrip.add(selectionSummaryLbl, BorderLayout.EAST);

        JPanel actionRow = new JPanel(new BorderLayout());
        actionRow.setOpaque(false);

        UITheme.GradientButton backBtn = UITheme.GradientButton.createGhost("← BACK", ACCENT_PURPLE, 12);
        backBtn.setPreferredSize(new Dimension(165, 40));
        backBtn.addActionListener(e -> {
            pulseTimer.stop();
            currentSelections.clear();
            dialog.dispose();
            startBookingFlow(group, showDate, requiredSeats, isPwdSenior, paymentMethod);
        });

        UITheme.GradientButton confirmBtn = new UITheme.GradientButton("Confirm", ACCENT_PURPLE, ACCENT_PINK, 12);
        confirmBtn.setPreferredSize(new Dimension(220, 40));
        confirmBtn.setFont(UITheme.fontBold(13));
        confirmBtn.addActionListener(e -> {
            if (currentSelections.size() < requiredSeats) {
                String reqSeatWord = (requiredSeats == 1) ? "seat" : "seats";
                ThemedDialog.showError(dialog, "Seat Selection Incomplete", "Please select exactly " + requiredSeats + " " + reqSeatWord + " before confirming.");
                return;
            }

            if (!isPwdSenior) {
                for (Point pt : currentSelections) {
                    if (seats[pt.x][pt.y] == 'P') {
                        ThemedDialog.showError(dialog, "Invalid Seat Selection", "Gold seats are strictly reserved for PWD / Senior Citizens!");
                        return;
                    }
                }
            }

            for (Point pt : currentSelections) {
                seats[pt.x][pt.y] = 'X';
            }

            pulseTimer.stop();
            dialog.dispose();
            showDoneDialog(ticket, billing, currentSelections, rowLetters, group);
        });

        actionRow.add(backBtn, BorderLayout.WEST);
        actionRow.add(confirmBtn, BorderLayout.EAST);

        southContainer.add(statusStrip);
        southContainer.add(actionRow);
        mainPanel.add(southContainer, BorderLayout.SOUTH);

        dialog.setContentPane(mainPanel);
        mainPanel.playDirectionalEntrance(0, 32, 0, 260);
        dialog.setVisible(true);
    }

    private JPanel createSeatLegendItem(Color swatchColor, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        item.setOpaque(false);

        JPanel swatch = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(swatchColor);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 5, 5);
                g2.setColor(new Color(255, 255, 255, 70));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 5, 5);
                g2.dispose();
            }
        };
        swatch.setOpaque(false);
        swatch.setPreferredSize(new Dimension(14, 14));

        JLabel lbl = new JLabel(text);
        lbl.setFont(UITheme.fontRegular(12));
        lbl.setForeground(TEXT_MUTED);

        item.add(swatch);
        item.add(lbl);
        return item;
    }

    // REALISTIC PERFORATED E-TICKET WITH PNG EXPORT
    private void showDoneDialog(MovieSchedule ticket, BillingCalculator billing, List<Point> selectedSeats, char[] rowLetters, MovieGroup group) {
        JDialog dialog = new JDialog(this, "SINE-SERVE Official E-Ticket", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        FadeSlidePanel outerShell = new FadeSlidePanel(null);
        outerShell.setLayout(new BoxLayout(outerShell, BoxLayout.Y_AXIS));
        outerShell.setBackground(BG_DARK);
        outerShell.setOpaque(true);
        outerShell.setBorder(new EmptyBorder(12, 20, 12, 20));

        UITheme.PillLabel confirmedBadge = new UITheme.PillLabel(
                "BOOKING CONFIRMED",
                new Color(16, 185, 129, 45),
                new Color(110, 231, 183),
                UITheme.PROMO_GREEN
        );
        confirmedBadge.setFont(UITheme.fontBold(12));
        confirmedBadge.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtext = new JLabel("Present this digital boarding pass at the auditorium entrance");
        subtext.setFont(UITheme.fontRegular(12));
        subtext.setForeground(TEXT_MUTED);
        subtext.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.PerforatedTicketCard ticketCard = new UITheme.PerforatedTicketCard(108);
        ticketCard.setLayout(new BoxLayout(ticketCard, BoxLayout.Y_AXIS));
        ticketCard.setBorder(new EmptyBorder(14, 20, 12, 20));

        int randomNo = 100000 + (int) (Math.random() * 900000);
        String refCode = "SS-" + randomNo;

        JPanel ticketBrandBar = new JPanel(new BorderLayout());
        ticketBrandBar.setOpaque(false);
        ticketBrandBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        JPanel brandLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandLeft.setOpaque(false);
        brandLeft.add(new JLabel(new UITheme.CinemaLogoIcon(22)));
        JLabel brandName = new JLabel("SINE-SERVE CINEMAS");
        brandName.setFont(UITheme.fontBold(12));
        brandName.setForeground(ACCENT_GLOW);
        brandLeft.add(brandName);

        JLabel refLbl = new JLabel("REF: " + refCode);
        refLbl.setFont(UITheme.fontMono(13));
        refLbl.setForeground(ACCENT_GOLD);

        ticketBrandBar.add(brandLeft, BorderLayout.WEST);
        ticketBrandBar.add(refLbl, BorderLayout.EAST);

        JPanel upperBody = new JPanel(new BorderLayout(18, 0));
        upperBody.setOpaque(false);

        UITheme.RoundedPanel posterContainer = new UITheme.RoundedPanel(14, PANEL_BG, ACCENT_PURPLE, false);
        posterContainer.setLayout(new GridBagLayout());
        posterContainer.setPreferredSize(new Dimension(136, 196));
        posterContainer.setMaximumSize(new Dimension(136, 196));

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 128, 188, 12);
        if (posterIcon != null) {
            posterLabel = new JLabel(posterIcon);
        } else {
            posterLabel = new JLabel("<html><center><b>SINE-SERVE</b><br><br>" + group.getTitle() + "</center></html>");
            posterLabel.setForeground(TEXT_MUTED);
        }
        posterContainer.add(posterLabel);

        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);

        boolean isPromoMovie = group.getTitle().contains("[CHRISTMAS PROMO]");
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        JLabel movieTitleLbl = new JLabel("<html><body style='width: 320px;'><b>" + cleanTitle + "</b></body></html>");
        movieTitleLbl.setFont(UITheme.fontDisplay(20));
        movieTitleLbl.setForeground(TEXT_WHITE);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        String formattedDate = ticket.getShowDate().format(dateFormatter);

        JPanel metaGrid = new JPanel(new GridLayout(2, 2, 12, 8));
        metaGrid.setOpaque(false);
        metaGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        metaGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 84));

        metaGrid.add(createReceiptDetailRow("SHOW DATE", formattedDate));
        metaGrid.add(createReceiptDetailRow("SHOWTIME", ticket.getShowTime()));
        metaGrid.add(createReceiptDetailRow("GUEST", isGuest ? "Guest" : currentUser));
        metaGrid.add(createReceiptDetailRow("PAYMENT", billing.getPaymentMethod()));

        String seatLabel = (selectedSeats.size() == 1) ? "ASSIGNED SEAT" : "ASSIGNED SEATS (" + selectedSeats.size() + ")";
        JLabel seatLblHeader = new JLabel(seatLabel);
        seatLblHeader.setFont(UITheme.fontBold(10));
        seatLblHeader.setForeground(TEXT_MUTED);
        seatLblHeader.setAlignmentX(Component.LEFT_ALIGNMENT);

        List<Point> sortedSeats = new ArrayList<>(selectedSeats);
        sortedSeats.sort(Comparator.comparingInt((Point p) -> p.x).thenComparingInt(p -> p.y));

        WrapLayoutContainer seatsChipContainer = new WrapLayoutContainer();
        for (Point p : sortedSeats) {
            String seatName = rowLetters[p.x] + String.valueOf(p.y + 1);
            UITheme.PillLabel chip = new UITheme.PillLabel(seatName, ACCENT_PURPLE, TEXT_WHITE, ACCENT_GLOW);
            chip.setFont(UITheme.fontBold(12));
            seatsChipContainer.add(chip);
        }
        seatsChipContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        movieTitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailsPanel.add(movieTitleLbl);

        if (isPromoMovie) {
            UITheme.PillLabel promoBadge = new UITheme.PillLabel("CHRISTMAS PROMO", UITheme.PROMO_RED, TEXT_WHITE, UITheme.PROMO_GREEN);
            promoBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            detailsPanel.add(promoBadge);
        }

        detailsPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        detailsPanel.add(metaGrid);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        detailsPanel.add(seatLblHeader);
        detailsPanel.add(seatsChipContainer);

        if (billing.getDiscountAmount() > 0) {
            UITheme.PillLabel discountBadge = new UITheme.PillLabel("PWD / Senior – 20% OFF Applied", new Color(22, 101, 52), new Color(134, 239, 172), UITheme.PROMO_GREEN);
            discountBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            detailsPanel.add(discountBadge);
        }

        upperBody.add(posterContainer, BorderLayout.WEST);
        upperBody.add(detailsPanel, BorderLayout.CENTER);

        UITheme.RoundedPanel priceBox = new UITheme.RoundedPanel(14, new Color(19, 14, 38), UITheme.BORDER_SUBTLE, false);
        priceBox.setLayout(new BoxLayout(priceBox, BoxLayout.Y_AXIS));
        priceBox.setBorder(new EmptyBorder(10, 14, 10, 14));
        priceBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 145));

        priceBox.add(createPriceRow("Ticket Price", String.format("PHP %.2f", billing.getSubtotal()), false, false));
        if (billing.getDiscountAmount() > 0) {
            priceBox.add(createPriceRow("PWD / Senior Citizen Discount (20%)", String.format("-PHP %.2f", billing.getDiscountAmount()), true, false));
        }

        if (billing.getOnlineFee() > 0) {
            priceBox.add(createPriceRow("Online Booking Fee", String.format("PHP %.2f", billing.getOnlineFee()), false, false));
        }

        JSeparator priceSep = new JSeparator();
        priceSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        priceSep.setForeground(UITheme.BORDER_SUBTLE);
        priceBox.add(Box.createRigidArea(new Dimension(0, 4)));
        priceBox.add(priceSep);
        priceBox.add(Box.createRigidArea(new Dimension(0, 4)));

        priceBox.add(createPriceRow("TOTAL PAID", String.format("PHP %.2f", billing.getFinalTotal()), false, true));

        BarcodePanel barcodePanel = new BarcodePanel();
        barcodePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel barcodeText = new JLabel(refCode + "  •  ADMIT " + selectedSeats.size());
        barcodeText.setFont(UITheme.fontMono(11));
        barcodeText.setForeground(TEXT_MUTED);
        barcodeText.setAlignmentX(Component.CENTER_ALIGNMENT);

        List<String> seatNames = new ArrayList<>();
        for (Point p : sortedSeats) {
            seatNames.add(rowLetters[p.x] + String.valueOf(p.y + 1));
        }

        UITheme.RoundedPanel seatMapBox = new UITheme.RoundedPanel(14, new Color(19, 14, 38), UITheme.BORDER_SUBTLE, false);
        seatMapBox.setLayout(new BorderLayout());
        seatMapBox.setBorder(new EmptyBorder(6, 10, 6, 10));
        seatMapBox.setPreferredSize(new Dimension(480, 178));
        seatMapBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 178));

        ReceiptSeatMapPanel miniMap = new ReceiptSeatMapPanel(seatNames, rowLetters.length, 9);
        seatMapBox.add(miniMap, BorderLayout.CENTER);

        ticketCard.add(ticketBrandBar);
        ticketCard.add(Box.createRigidArea(new Dimension(0, 8)));
        ticketCard.add(upperBody);
        ticketCard.add(Box.createRigidArea(new Dimension(0, 8)));
        ticketCard.add(seatMapBox);
        ticketCard.add(Box.createRigidArea(new Dimension(0, 8)));
        ticketCard.add(priceBox);
        ticketCard.add(Box.createRigidArea(new Dimension(0, 18)));
        ticketCard.add(barcodePanel);
        ticketCard.add(Box.createRigidArea(new Dimension(0, 2)));
        ticketCard.add(barcodeText);

        // Action Buttons: Save Ticket PNG + Done
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton savePngBtn = UITheme.GradientButton.createGhost("Save E-Ticket (.PNG)", ACCENT_GOLD, 14);
        savePngBtn.setPreferredSize(new Dimension(190, 42));
        savePngBtn.setFont(UITheme.fontBold(13));
        savePngBtn.setForeground(ACCENT_GOLD);
        savePngBtn.addActionListener(e -> exportTicketAsPng(dialog, ticketCard, refCode));

        UITheme.GradientButton closeBtn = new UITheme.GradientButton("Done", ACCENT_PURPLE, ACCENT_PINK, 14);
        closeBtn.setPreferredSize(new Dimension(220, 42));
        closeBtn.setFont(UITheme.fontBold(14));
        closeBtn.addActionListener(e -> dialog.dispose());

        actionRow.add(savePngBtn);
        actionRow.add(closeBtn);

        outerShell.add(confirmedBadge);
        outerShell.add(Box.createRigidArea(new Dimension(0, 6)));
        outerShell.add(subtext);
        outerShell.add(Box.createRigidArea(new Dimension(0, 14)));
        outerShell.add(ticketCard);
        outerShell.add(Box.createRigidArea(new Dimension(0, 16)));
        outerShell.add(actionRow);

        String seatsText = getSeatsSummaryText(sortedSeats, rowLetters);
        boolean alreadySaved = false;
        for (BookedTicketRecord r : bookedTickets) {
            if (r.movieTitle.equals(cleanTitle) && r.showDate.equals(formattedDate) && r.showTime.equals(ticket.getShowTime())
                    && r.seatsSummary.equals(seatsText)) {
                alreadySaved = true;
                break;
            }
        }
        if (!alreadySaved) {
            List<Point> savedSeatsCopy = new ArrayList<>(selectedSeats);
            String paidText = String.format("PHP %.2f", billing.getFinalTotal());
            bookedTickets.add(new BookedTicketRecord(
                    refCode,
                    cleanTitle,
                    formattedDate,
                    ticket.getShowTime(),
                    seatsText,
                    paidText,
                    () -> showDoneDialog(ticket, billing, savedSeatsCopy, rowLetters, group)
            ));
            if (myTicketsBtn != null) {
                myTicketsBtn.setText("My E-Tickets (" + bookedTickets.size() + ")");
            }
            refreshFilteredMovies(false);
        }

        dialog.setContentPane(outerShell);
        dialog.pack();
        dialog.setSize(680, dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        outerShell.playDirectionalEntrance(0, 32, 0, 260);
        dialog.setVisible(true);
    }

    private void exportTicketAsPng(Component parent, JComponent ticketCard, String refCode) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save E-Ticket Image");
        chooser.setFileFilter(new FileNameExtensionFilter("PNG Image (*.png)", "png"));
        chooser.setSelectedFile(new File("SINE-SERVE_" + refCode + ".png"));

        if (chooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".png")) {
                file = new File(file.getParentFile(), file.getName() + ".png");
            }
            try {
                int pad = 20;
                BufferedImage img = new BufferedImage(
                        ticketCard.getWidth() + pad * 2,
                        ticketCard.getHeight() + pad * 2,
                        BufferedImage.TYPE_INT_ARGB
                );
                Graphics2D g2 = img.createGraphics();
                UITheme.enableAntiAliasing(g2);
                g2.setColor(BG_DARK);
                g2.fillRect(0, 0, img.getWidth(), img.getHeight());
                g2.translate(pad, pad);
                ticketCard.paint(g2);
                g2.dispose();

                ImageIO.write(img, "png", file);
                ThemedDialog.showInfo(parent, "E-Ticket Saved", "Your boarding pass was saved to:\n" + file.getAbsolutePath());
            } catch (Exception ex) {
                ThemedDialog.showError(parent, "Export Failed", "Could not save the E-Ticket image: " + ex.getMessage());
            }
        }
    }

    private String getSeatsSummaryText(List<Point> sortedSeats, char[] rowLetters) {
        List<String> names = new ArrayList<>();
        for (Point p : sortedSeats) {
            names.add(rowLetters[p.x] + String.valueOf(p.y + 1));
        }
        return String.join(", ", names);
    }

    private JPanel createReceiptDetailRow(String label, String value) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(UITheme.fontBold(10));
        lbl.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(UITheme.fontBold(13));
        val.setForeground(TEXT_WHITE);

        row.add(lbl);
        row.add(val);
        return row;
    }

    private JPanel createPriceRow(String label, String value, boolean isDiscount, boolean isTotal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        JLabel lbl = new JLabel(label);
        lbl.setFont(isTotal ? UITheme.fontBold(15) : UITheme.fontRegular(12));
        lbl.setForeground(isTotal ? TEXT_WHITE : TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(UITheme.fontBold(isTotal ? 17 : 13));

        if (isTotal) {
            val.setForeground(ACCENT_GOLD);
        } else if (isDiscount) {
            val.setForeground(new Color(134, 239, 172));
        } else {
            val.setForeground(TEXT_WHITE);
        }

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    private static class BarcodePanel extends JPanel {
        public BarcodePanel() {
            setPreferredSize(new Dimension(240, 44));
            setMaximumSize(new Dimension(240, 44));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int width = getWidth();
            int height = getHeight();
            int[] pattern = {3, 1, 2, 1, 4, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3, 1, 2, 3, 2, 1, 3};

            int totalWidth = 0;
            for (int p : pattern) totalWidth += p + 2;
            totalWidth -= 2;
            int currentX = Math.max(0, (width - totalWidth) / 2);

            g2.setColor(TEXT_WHITE);
            for (int i = 0; i < pattern.length; i++) {
                if (i % 2 == 0) g2.fillRect(currentX, 0, pattern[i], height);
                currentX += pattern[i] + 2;
            }
            g2.dispose();
        }
    }

    private static class WrapLayoutContainer extends JPanel {
        private static final int WRAP_WIDTH = 320;

        public WrapLayoutContainer() {
            setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            int x = 0, rowH = 0, totalH = 4;
            for (Component c : getComponents()) {
                Dimension d = c.getPreferredSize();
                if (x > 0 && x + d.width + 4 > WRAP_WIDTH) {
                    totalH += rowH + 4;
                    x = 0;
                    rowH = 0;
                }
                x += d.width + 4;
                rowH = Math.max(rowH, d.height);
            }
            totalH += rowH + 4;
            return new Dimension(WRAP_WIDTH, totalH);
        }

        @Override
        public Dimension getMaximumSize() {
            return getPreferredSize();
        }
    }

    private char[][] createNewSeatPlan() {
        char[][] seats = new char[10][9];
        for (int i = 0; i < 10; i++) {
            for (int j = 0; j < 9; j++) {
                seats[i][j] = '*';
            }
        }
        seats[5][0] = 'P';
        seats[5][5] = 'P';
        seats[5][8] = 'P';
        return seats;
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
        UIManager.put("ToolTip.background", new Color(28, 22, 56));
        UIManager.put("ToolTip.foreground", new Color(243, 244, 246));
        UIManager.put("ToolTip.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("ToolTip.border", BorderFactory.createCompoundBorder(
        BorderFactory.createLineBorder(new Color(168, 85, 247), 1),
        BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        ToolTipManager.sharedInstance().setInitialDelay(220);
        ToolTipManager.sharedInstance().setReshowDelay(250);
        ToolTipManager.sharedInstance().setDismissDelay(6000);

        SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
    }

private static class ReceiptSeatMapPanel extends JPanel {
    private final Set<String> bookedSeats;
    private final int rows;
    private final int cols;

    public ReceiptSeatMapPanel(Collection<String> selectedSeats, int rows, int cols) {
        this.bookedSeats = new HashSet<>();
        if (selectedSeats != null) {
            for (String s : selectedSeats) {
                this.bookedSeats.add(s.trim().toUpperCase());
            }
        }
        this.rows = Math.max(1, rows);
        this.cols = Math.max(1, cols);
        setOpaque(false);
        setPreferredSize(new Dimension(340, 166));
        setMaximumSize(new Dimension(420, 174));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int topY = 6;

        // 1. Curved Cinema Screen Bar
        int screenW = Math.min(230, w - 60);
        int screenX = (w - screenW) / 2;
        g2.setColor(new Color(168, 85, 247, 180));
        g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(screenX, topY, screenW, 14, 15, 150);

        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.setColor(new Color(180, 175, 210));
        FontMetrics fm = g2.getFontMetrics();
        String screenTxt = "CINEMA SCREEN";
        g2.drawString(screenTxt, (w - fm.stringWidth(screenTxt)) / 2, topY + 15);

        // 2. Dynamically Size Rows so A..J + Legend Always Fit
        int startY = topY + 24;
        int legendSpace = 22;
        int availH = Math.max(80, h - startY - legendSpace);

        int gapY = 3;
        int seatSize = Math.max(8, Math.min(12, (availH - (rows - 1) * gapY) / rows));
        int gapX = 4;
        int aisleGap = 14;

        int aisleAfter = 4;
        int totalGridW = (cols * seatSize) + ((cols - 1) * gapX) + aisleGap;
        int startX = (w - totalGridW) / 2;

        g2.setFont(new Font("SansSerif", Font.BOLD, 8));

        for (int r = 0; r < rows; r++) {
            char rowChar = (char) ('A' + r);
            int y = startY + r * (seatSize + gapY);

            // Row letter label on left
            g2.setColor(new Color(148, 144, 175));
            g2.drawString(String.valueOf(rowChar), startX - 15, y + seatSize - 1);

            for (int c = 1; c <= cols; c++) {
                int extraAisle = (c > aisleAfter) ? aisleGap : 0;
                int x = startX + (c - 1) * (seatSize + gapX) + extraAisle;
                String seatId = rowChar + String.valueOf(c);
                boolean isMine = bookedSeats.contains(seatId);

                if (isMine) {
                    // Glow behind reserved seat
                    g2.setColor(new Color(251, 191, 36, 85));
                    g2.fillRoundRect(x - 2, y - 2, seatSize + 4, seatSize + 4, 5, 5);

                    // Highlighted Reserved Seat (Gold)
                    g2.setColor(new Color(251, 191, 36));
                    g2.fillRoundRect(x, y, seatSize, seatSize, 3, 3);
                } else {
                    // Unselected Auditorium Seat
                    g2.setColor(new Color(52, 44, 88));
                    g2.fillRoundRect(x, y, seatSize, seatSize, 3, 3);
                }
            }
        }

        // 3. Legend at Bottom
        int legendY = startY + rows * (seatSize + gapY) + 12;
        int legendX = (w - 175) / 2;

        g2.setColor(new Color(251, 191, 36));
        g2.fillRoundRect(legendX, legendY - 8, 9, 9, 3, 3);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 9));
        g2.drawString("Your Seat(s)", legendX + 14, legendY);

        g2.setColor(new Color(52, 44, 88));
        g2.fillRoundRect(legendX + 92, legendY - 8, 9, 9, 3, 3);
        g2.setColor(new Color(160, 155, 190));
        g2.drawString("Other Seats", legendX + 106, legendY);

        g2.dispose();
    }
}

}