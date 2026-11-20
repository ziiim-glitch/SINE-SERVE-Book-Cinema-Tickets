import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CinemaUI extends JFrame {

    // Palette
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
    private static final Color DISABLED_BG = UITheme.DISABLED_BG;

    // Seats
    private static final Color SEAT_AVAILABLE = new Color(55, 48, 107);
    private static final Color SEAT_SELECTED = new Color(168, 85, 247);
    private static final Color SEAT_RESERVED = new Color(180, 40, 40);
    private static final Color SEAT_PWD = new Color(202, 138, 4);
    private static final Color SEAT_PWD_DISABLED = new Color(90, 65, 10);

    private final CinemaManager cinemaManager;
    private final Map<String, char[][]> seatPlans;
    private static final Map<String, ImageIcon> posterCache = new HashMap<>();

    private JScrollPane carouselScrollPane;
    private String currentUser;
    private final boolean isGuest;

    // Search & Filter State
    private String activeFilter = "ALL"; // ALL, REGULAR, PROMO
    private String searchQuery = "";
    private JPanel featuredSectionWrapper;
    private JPanel regularSectionWrapper;
    private JPanel promoSectionWrapper;
    private JPanel regularGridPanel;
    private JPanel promoGridPanel;

    public CinemaUI(String currentUser, boolean isGuest) {
        if (currentUser == null || currentUser.trim().isEmpty()) {
            throw new IllegalArgumentException("A valid user or Guest session is required.");
        }
        this.currentUser = currentUser;
        this.isGuest = isGuest;
        this.cinemaManager = new CinemaManager();
        this.seatPlans = new HashMap<>();

        setTitle("SINE-SERVE | Book • Watch • Enjoy");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1280, 900);
        setMinimumSize(new Dimension(960, 720));
        setLocationRelativeTo(null);
        getContentPane().setBackground(new Color(0x12, 0x0E, 0x21));
        setLayout(new BorderLayout());

        verifyPosters();

        List<MovieGroup> allMovies = new ArrayList<>(cinemaManager.getRegularGroups());
        allMovies.addAll(cinemaManager.getPromoGroups());

        JPanel mainContent = new ScrollablePanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(new Color(0x12, 0x0E, 0x21));
        mainContent.setOpaque(true);
        mainContent.setBorder(new EmptyBorder(0, 24, 28, 24));

        // 1. Gradient Header with Rounded Bottom Corners
        mainContent.add(createHeaderBanner());
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));

        // 2. Search + Filter Bar
        mainContent.add(createFilterAndSearchBar());
        mainContent.add(Box.createRigidArea(new Dimension(0, 24)));

        // 3. Featured Carousel Section
        featuredSectionWrapper = createTopBrowsingSection(allMovies);
        mainContent.add(featuredSectionWrapper);
        mainContent.add(Box.createRigidArea(new Dimension(0, 24)));

        // 4. Available Movies Section
        regularSectionWrapper = new JPanel();
        regularSectionWrapper.setLayout(new BoxLayout(regularSectionWrapper, BoxLayout.Y_AXIS));
        regularSectionWrapper.setOpaque(false);
        regularSectionWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);

        regularSectionWrapper.add(createSectionDivider());
        regularSectionWrapper.add(Box.createRigidArea(new Dimension(0, 20)));
        regularSectionWrapper.add(createMovieSectionTitle(
                "Available Movies for Reservation",
                isGuest ? "Guest Mode — Browse Only" : "Select a movie to reserve seats",
                ACCENT_PURPLE,
                ACCENT_PINK
        ));
        regularSectionWrapper.add(Box.createRigidArea(new Dimension(0, 14)));
        regularGridPanel = createMovieGridSection(cinemaManager.getRegularGroups(), false);
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
        promoGridPanel = createMovieGridSection(cinemaManager.getPromoGroups(), true);
        promoSectionWrapper.add(promoGridPanel);
        mainContent.add(promoSectionWrapper);

        // Absorb extra vertical space when maximized
        mainContent.add(Box.createVerticalGlue());

        JScrollPane pageScrollPane = new JScrollPane(mainContent);
        pageScrollPane.setBorder(null);
        pageScrollPane.setBackground(new Color(0x12, 0x0E, 0x21));
        pageScrollPane.setOpaque(true);
        pageScrollPane.getViewport().setBackground(new Color(0x12, 0x0E, 0x21));
        pageScrollPane.getViewport().setOpaque(true);
        pageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(18);
        pageScrollPane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pageScrollPane.getVerticalScrollBar().setUI(new SlimPurpleScrollBarUI());
        add(pageScrollPane, BorderLayout.CENTER);

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                refreshFilteredMovies();
            }
        });
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

    private static class ScrollablePanel extends JPanel implements Scrollable {
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

    // =========================================================================
    // 1. HEADER: GRADIENT BAR WITH ROUNDED BOTTOM CORNERS, LOGO, AVATAR & LOGOUT
    // =========================================================================
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
                        0, 0, new Color(46, 26, 94),
                        w, h, new Color(26, 18, 52)
                );
                g2.setPaint(gp);
                g2.fill(path);

                g2.setColor(UITheme.BORDER_SUBTLE);
                g2.draw(path);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(new EmptyBorder(14, 24, 14, 24));
        banner.setPreferredSize(new Dimension(0, 78));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 78));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        leftBox.setOpaque(false);

        JLabel iconLbl = new JLabel(new UITheme.CinemaLogoIcon(38));

        JLabel logo = new JLabel("SINE-SERVE");
        logo.setFont(UITheme.fontDisplay(24));
        logo.setForeground(TEXT_WHITE);

        JLabel sepDot = new JLabel("•");
        sepDot.setFont(UITheme.fontBold(18));
        sepDot.setForeground(ACCENT_LIGHT);

        JLabel subTag = new JLabel(isGuest ? "Guest Browsing" : "Cinema Ticket Booking");
        subTag.setFont(UITheme.fontRegular(13));
        subTag.setForeground(ACCENT_GLOW);

        leftBox.add(iconLbl);
        leftBox.add(logo);
        leftBox.add(sepDot);
        leftBox.add(subTag);

        JPanel rightBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        rightBox.setOpaque(false);

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
                g2.setColor(new Color(255, 255, 255, 110));
                g2.draw(new Ellipse2D.Double(0, 0, getWidth() - 1, getHeight() - 1));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        avatarCircle.setOpaque(false);
        avatarCircle.setPreferredSize(new Dimension(34, 34));
        JLabel initLbl = new JLabel(initial);
        initLbl.setFont(UITheme.fontBold(14));
        initLbl.setForeground(TEXT_WHITE);
        avatarCircle.add(initLbl);

        JLabel userLbl = new JLabel("Welcome, " + displayName);
        userLbl.setFont(UITheme.fontBold(14));
        userLbl.setForeground(TEXT_WHITE);

        UITheme.GradientButton logoutBtn = UITheme.GradientButton.createGhost(
                isGuest ? "Log In / Sign Up" : "Logout",
                ACCENT_LIGHT,
                16
        );
        logoutBtn.setPreferredSize(new Dimension(isGuest ? 136 : 94, 34));
        logoutBtn.setFont(UITheme.fontBold(12));
        logoutBtn.addActionListener(e -> {
            this.currentUser = null;
            dispose();
            SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
        });

        rightBox.add(avatarCircle);
        rightBox.add(userLbl);
        rightBox.add(Box.createRigidArea(new Dimension(4, 0)));
        rightBox.add(logoutBtn);

        banner.add(leftBox, BorderLayout.WEST);
        banner.add(rightBox, BorderLayout.EAST);
        return banner;
    }

    // =========================================================================
    // 5. TOP SEARCH + FILTER BAR (ALL / NOW SHOWING / PROMOS)
    // =========================================================================
    private JPanel createFilterAndSearchBar() {
        JPanel bar = new JPanel(new BorderLayout(16, 0));
        bar.setOpaque(false);
        bar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        bar.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel chipRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        chipRow.setOpaque(false);

        UITheme.GradientButton allChip = new UITheme.GradientButton("All", ACCENT_PURPLE, ACCENT_PINK, 20);
        UITheme.GradientButton regularChip = new UITheme.GradientButton("Now Showing", CARD_BG, CARD_BG, 20);
        UITheme.GradientButton promoChip = new UITheme.GradientButton("Promos", CARD_BG, CARD_BG, 20);

        allChip.setPreferredSize(new Dimension(92, 36));
        regularChip.setPreferredSize(new Dimension(136, 36));
        promoChip.setPreferredSize(new Dimension(112, 36));

        Runnable updateChipStyles = () -> {
            allChip.setGradientColors(
                    activeFilter.equals("ALL") ? ACCENT_PURPLE : CARD_BG,
                    activeFilter.equals("ALL") ? ACCENT_PINK : CARD_BG
            );
            regularChip.setGradientColors(
                    activeFilter.equals("REGULAR") ? ACCENT_PURPLE : CARD_BG,
                    activeFilter.equals("REGULAR") ? ACCENT_PINK : CARD_BG
            );
            promoChip.setGradientColors(
                    activeFilter.equals("PROMO") ? UITheme.PROMO_RED : CARD_BG,
                    activeFilter.equals("PROMO") ? UITheme.PROMO_GREEN : CARD_BG
            );
        };

        allChip.addActionListener(e -> {
            activeFilter = "ALL";
            updateChipStyles.run();
            refreshFilteredMovies();
        });

        regularChip.addActionListener(e -> {
            activeFilter = "REGULAR";
            updateChipStyles.run();
            refreshFilteredMovies();
        });

        promoChip.addActionListener(e -> {
            activeFilter = "PROMO";
            updateChipStyles.run();
            refreshFilteredMovies();
        });

        chipRow.add(allChip);
        chipRow.add(regularChip);
        chipRow.add(promoChip);

        UITheme.IconTextField searchBox = new UITheme.IconTextField(
                "Search movies by title...",
                UITheme.VectorIcon.Type.SEARCH
        );
        searchBox.setPreferredSize(new Dimension(310, 40));
        searchBox.getField().getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { onSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { onSearch(); }
            private void onSearch() {
                searchQuery = searchBox.getText().trim().toLowerCase();
                refreshFilteredMovies();
            }
        });

        bar.add(chipRow, BorderLayout.WEST);
        bar.add(searchBox, BorderLayout.EAST);
        return bar;
    }

    private void refreshFilteredMovies() {
        boolean showFeatured = activeFilter.equals("ALL") && searchQuery.isEmpty();
        featuredSectionWrapper.setVisible(showFeatured);

        boolean showRegular = activeFilter.equals("ALL") || activeFilter.equals("REGULAR");
        boolean showPromo = activeFilter.equals("ALL") || activeFilter.equals("PROMO");

        regularSectionWrapper.setVisible(showRegular);
        promoSectionWrapper.setVisible(showPromo);

        int cols = getWidth() >= 1450 ? 4 : (getWidth() < 1050 ? 2 : 3);

        if (showRegular) {
            rebuildFilteredGrid(regularGridPanel, cinemaManager.getRegularGroups(), false, cols);
        }
        if (showPromo) {
            rebuildFilteredGrid(promoGridPanel, cinemaManager.getPromoGroups(), true, cols);
        }

        revalidate();
        repaint();
    }

    private void rebuildFilteredGrid(JPanel gridPanel, List<MovieGroup> groups, boolean isPromo, int cols) {
        gridPanel.removeAll();
        gridPanel.setLayout(new GridLayout(0, cols, 18, 18));

        int added = 0;
        for (MovieGroup g : groups) {
            if (searchQuery.isEmpty() || g.getTitle().toLowerCase().contains(searchQuery)) {
                gridPanel.add(new MovieCard(g, isPromo));
                added++;
            }
        }

        if (added == 0) {
            gridPanel.setLayout(new FlowLayout(FlowLayout.LEFT));
            JLabel empty = new JLabel("No matching movies found for \"" + searchQuery + "\".");
            empty.setFont(UITheme.fontRegular(14));
            empty.setForeground(TEXT_MUTED);
            gridPanel.add(empty);
        }
    }

    // =========================================================================
    // 2. FEATURED CAROUSEL (ROUNDED CARDS, BADGE, GOLD PILL, GRAPHICS2D ARROWS)
    // =========================================================================
    private JPanel createTopBrowsingSection(List<MovieGroup> movies) {
        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleWithBar = createMovieSectionTitle(
                "Featured Movie Showcase",
                "Browse Now Showing & Seasonal Picks",
                ACCENT_PURPLE,
                ACCENT_PINK
        );

        JPanel navButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        navButtons.setOpaque(false);

        UITheme.GradientButton leftArrowBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 18);
        leftArrowBtn.setPreferredSize(new Dimension(38, 34));
        leftArrowBtn.setMargin(new Insets(0, 0, 0, 0));
        leftArrowBtn.setIcon(new ChevronArrowIcon(true, 12, TEXT_WHITE));
        leftArrowBtn.setToolTipText("Scroll Left");

        UITheme.GradientButton rightArrowBtn = UITheme.GradientButton.createGhost("", ACCENT_LIGHT, 18);
        rightArrowBtn.setPreferredSize(new Dimension(38, 34));
        rightArrowBtn.setMargin(new Insets(0, 0, 0, 0));
        rightArrowBtn.setIcon(new ChevronArrowIcon(false, 12, TEXT_WHITE));
        rightArrowBtn.setToolTipText("Scroll Right");

        leftArrowBtn.addActionListener(e -> smoothScrollCarousel(-300));
        rightArrowBtn.addActionListener(e -> smoothScrollCarousel(300));

        navButtons.add(leftArrowBtn);
        navButtons.add(rightArrowBtn);

        headerPanel.add(titleWithBar, BorderLayout.WEST);
        headerPanel.add(navButtons, BorderLayout.EAST);
        container.add(headerPanel, BorderLayout.NORTH);

        JPanel carouselGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        carouselGrid.setOpaque(false);

        for (MovieGroup group : movies) {
            carouselGrid.add(createFeaturedCarouselCard(group));
        }

        carouselScrollPane = new JScrollPane(carouselGrid);
        carouselScrollPane.setBorder(null);
        carouselScrollPane.setOpaque(false);
        carouselScrollPane.getViewport().setOpaque(false);
        carouselScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        carouselScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        carouselScrollPane.setPreferredSize(new Dimension(0, 422));

        JScrollBar hBar = carouselScrollPane.getHorizontalScrollBar();
        hBar.setPreferredSize(new Dimension(0, 8));
        hBar.setUI(new SlimPurpleScrollBarUI());

        MouseAdapter scrollAdapter = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                JScrollBar hScrollBar = carouselScrollPane.getHorizontalScrollBar();
                int scrollAmount = e.getUnitsToScroll() * 28;
                hScrollBar.setValue(hScrollBar.getValue() + scrollAmount);
            }
        };

        carouselScrollPane.addMouseWheelListener(scrollAdapter);
        carouselGrid.addMouseWheelListener(scrollAdapter);

        container.add(carouselScrollPane, BorderLayout.CENTER);
        return container;
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

    private void smoothScrollCarousel(int delta) {
        if (carouselScrollPane == null) return;
        JScrollBar bar = carouselScrollPane.getHorizontalScrollBar();
        int start = bar.getValue();
        int target = Math.max(bar.getMinimum(), Math.min(bar.getMaximum() - bar.getVisibleAmount(), start + delta));
        final int[] step = {0};
        Timer scrollTimer = new Timer(12, e -> {
            step[0]++;
            float t = Math.min(1f, step[0] / 12f);
            float ease = 1f - (1f - t) * (1f - t);
            bar.setValue(Math.round(start + (target - start) * ease));
            if (t >= 1f) ((Timer) e.getSource()).stop();
        });
        scrollTimer.start();
    }

    private static class SlimPurpleScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = ACCENT_LIGHT;
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
            Color cThumb = isDragging ? ACCENT_GLOW : (isThumbRollover() ? ACCENT_LIGHT : ACCENT_PURPLE);
            g2.setColor(cThumb);
            g2.fillRoundRect(thumbBounds.x + 1, thumbBounds.y + 1, thumbBounds.width - 2, thumbBounds.height - 2, 8, 8);
            g2.dispose();
        }
    }

    private JPanel createFeaturedCarouselCard(MovieGroup group) {
        boolean isPromo = group.getTitle().contains("[CHRISTMAS PROMO]") || group.getPrice() <= 110.0;
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();

        int cardWidth = 252;
        int innerWidth = cardWidth - 28; // 224px

        UITheme.RoundedPanel card = new UITheme.RoundedPanel(
                20,
                CARD_BG,
                UITheme.BORDER_SUBTLE,
                true
        );
        card.enableHoverEffect(ACCENT_LIGHT);
        card.setPreferredSize(new Dimension(cardWidth, 396));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(12, 14, 14, 14));

        // 1. Rounded Poster (Arc 20) + Compact Top-Left Badge at (8, 8)
        JLayeredPane posterLayer = new JLayeredPane();
        posterLayer.setPreferredSize(new Dimension(222, 222));
        posterLayer.setMaximumSize(new Dimension(222, 222));
        posterLayer.setMinimumSize(new Dimension(222, 222));
        posterLayer.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 222, 222, 20);
        if (posterIcon != null) {
            posterLabel = new JLabel(posterIcon);
        } else {
            posterLabel = new JLabel("<html><div style='width:190px; text-align:center;'><b>SINE-SERVE</b><br><br>" + cleanTitle + "</div></html>", SwingConstants.CENTER);
            posterLabel.setOpaque(true);
            posterLabel.setBackground(PANEL_BG);
            posterLabel.setForeground(TEXT_MUTED);
        }
        posterLabel.setBounds(0, 0, 222, 222);
        posterLayer.add(posterLabel, JLayeredPane.DEFAULT_LAYER);

        UITheme.PillLabel statusBadge = new UITheme.PillLabel(
                isPromo ? "PROMO" : "NOW SHOWING",
                new Color(18, 14, 33, 185),
                isPromo ? new Color(252, 165, 165) : ACCENT_GLOW,
                isPromo ? new Color(225, 29, 72, 175) : new Color(168, 85, 247, 175)
        );
        statusBadge.setFont(UITheme.fontBold(9));
        statusBadge.setBorder(new EmptyBorder(2, 8, 2, 8));
        statusBadge.setBounds(8, 8, isPromo ? 56 : 88, 19);
        posterLayer.add(statusBadge, JLayeredPane.PALETTE_LAYER);

        // 2. Pixel-Exact Centered 2-Line Title Component
        WrappedTitleComponent titleComp = new WrappedTitleComponent(
                cleanTitle,
                UITheme.fontBold(15),
                TEXT_WHITE,
                true,
                innerWidth,
                42
        );
        titleComp.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. Centered Date Row
        JPanel dateRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        dateRow.setOpaque(false);
        dateRow.setPreferredSize(new Dimension(innerWidth, 18));
        dateRow.setMaximumSize(new Dimension(innerWidth, 18));
        dateRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel dateIcon = new JLabel(new UITheme.VectorIcon(UITheme.VectorIcon.Type.CALENDAR, 13, TEXT_MUTED));
        JLabel dateLbl = new JLabel(group.getDateRange());
        dateLbl.setFont(UITheme.fontRegular(12));
        dateLbl.setForeground(TEXT_MUTED);
        dateRow.add(dateIcon);
        dateRow.add(dateLbl);

        // 4. Centered Showtime Row
        JPanel timeRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        timeRow.setOpaque(false);
        timeRow.setPreferredSize(new Dimension(innerWidth, 18));
        timeRow.setMaximumSize(new Dimension(innerWidth, 18));
        timeRow.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel timeIcon = new JLabel(new UITheme.VectorIcon(UITheme.VectorIcon.Type.CLOCK, 13, TEXT_MUTED));
        JLabel timeLbl = new JLabel(group.getTime());
        timeLbl.setFont(UITheme.fontRegular(12));
        timeLbl.setForeground(TEXT_MUTED);
        timeRow.add(timeIcon);
        timeRow.add(timeLbl);

        // 5. Centered Gold Price Pill
        UITheme.PillLabel goldPricePill = new UITheme.PillLabel(
                "PHP " + String.format("%.2f", group.getPrice()),
                new Color(251, 191, 36, 42),
                ACCENT_GOLD,
                new Color(251, 191, 36, 140)
        );
        goldPricePill.setFont(UITheme.fontBold(13));
        goldPricePill.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(posterLayer);
        card.add(Box.createRigidArea(new Dimension(0, 10)));
        card.add(titleComp);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(dateRow);
        card.add(Box.createRigidArea(new Dimension(0, 3)));
        card.add(timeRow);
        card.add(Box.createVerticalGlue());
        card.add(goldPricePill);

        return card;
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

    // =========================================================================
    // SECTION DIVIDERS & HEADERS
    // =========================================================================
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
        mainLabel.setFont(UITheme.fontDisplay(22));
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
                g2.setColor(new Color(225, 29, 72, 160));
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
        title.setFont(UITheme.fontDisplay(22));
        title.setForeground(TEXT_WHITE);
        left.add(snowIcon);
        left.add(title);

        UITheme.PillLabel promoTag = new UITheme.PillLabel(
                "Holiday Special • PHP 102.00",
                new Color(16, 185, 129, 75),
                new Color(167, 243, 208),
                UITheme.PROMO_GREEN
        );
        promoTag.setFont(UITheme.fontBold(12));

        banner.add(left, BorderLayout.WEST);
        banner.add(promoTag, BorderLayout.EAST);
        return banner;
    }

    private JPanel createMovieGridSection(List<MovieGroup> groups, boolean isPromo) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (MovieGroup group : groups) {
            grid.add(new MovieCard(group, isPromo));
        }

        return grid;
    }

    // =========================================================================
    // 3 & 4. REUSABLE MOVIE CARD (ROUNDED, HOVER GLOW, ICONS, GOLD PILL, RIBBON)
    // =========================================================================
    private class MovieCard extends UITheme.RoundedPanel {
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
            setPreferredSize(new Dimension(0, 276));

            String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();

            JLayeredPane posterLayer = new JLayeredPane();
            posterLayer.setPreferredSize(new Dimension(156, 242));
            posterLayer.setMinimumSize(new Dimension(156, 242));

            JLabel posterLabel;
            ImageIcon posterIcon = loadPoster(group.getPosterPath(), 156, 242, 16);
            if (posterIcon != null) {
                posterLabel = new JLabel(posterIcon);
            } else {
                posterLabel = new JLabel("<html><div style='width:140px; text-align:center;'><b>SINE-SERVE</b><br><br>" + cleanTitle + "</div></html>", SwingConstants.CENTER);
                posterLabel.setOpaque(true);
                posterLabel.setBackground(PANEL_BG);
                posterLabel.setForeground(TEXT_MUTED);
            }
            posterLabel.setBounds(0, 0, 156, 242);
            posterLayer.add(posterLabel, JLayeredPane.DEFAULT_LAYER);

            if (isPromo) {
                UITheme.DiagonalRibbon ribbon = new UITheme.DiagonalRibbon("PROMO");
                ribbon.setBounds(68, 0, 88, 88);
                posterLayer.add(ribbon, JLayeredPane.PALETTE_LAYER);
            }

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
                actionBtn = UITheme.GradientButton.createGhost("Browse Only", UITheme.BORDER_SUBTLE, 14);
                actionBtn.setIcon(new UITheme.VectorIcon(UITheme.VectorIcon.Type.LOCK, 14, TEXT_MUTED));
                actionBtn.setForeground(TEXT_MUTED);
                actionBtn.setToolTipText("Log in or create an account to reserve tickets.");
                actionBtn.addActionListener(e -> ThemedDialog.showInfo(
                        CinemaUI.this,
                        "Login Required",
                        "Guest mode is browse-only. Please log in or create an account to reserve tickets."
                ));
            } else {
                actionBtn = new UITheme.GradientButton(
                        "Reserve",
                        isPromo ? UITheme.PROMO_RED : ACCENT_PURPLE,
                        isPromo ? UITheme.PROMO_GREEN : ACCENT_PINK,
                        14
                );
                actionBtn.addActionListener(e -> startBookingFlow(group, null, 1, false, "GCash"));
            }

            actionBtn.setPreferredSize(new Dimension(180, 40));
            actionBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
            actionBtn.setFont(UITheme.fontBold(13));
            actionBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

            infoPanel.add(titleComp);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPanel.add(dateLbl);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));
            infoPanel.add(timeLbl);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 12)));
            infoPanel.add(priceRow);
            infoPanel.add(Box.createVerticalGlue());
            infoPanel.add(actionBtn);

            add(posterLayer, BorderLayout.WEST);
            add(infoPanel, BorderLayout.CENTER);
        }
    }

    // =========================================================================
    // BOOKING, SEAT SELECTION & RECEIPT DIALOGS
    // =========================================================================
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

        JDialog dialog = new JDialog(this, "Select Date & Booking Details", true);
        dialog.setSize(520, 670);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(22, 26, 22, 26));

        JLabel title = new JLabel("Select Date for " + group.getTitle().replace("[CHRISTMAS PROMO]", "").trim());
        title.setFont(UITheme.fontBold(18));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        LocalDate[] range = parseMovieDateRange(group.getDateRange());
        final LocalDate[] chosenDateHolder = {initialDate != null ? initialDate : range[0]};

        JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(UITheme.fontBold(12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField ticketTextField = new JTextField(String.valueOf(initialQuantity));
        ticketTextField.setPreferredSize(new Dimension(80, 36));
        ticketTextField.setMaximumSize(new Dimension(80, 36));
        ticketTextField.setBackground(PANEL_BG);
        ticketTextField.setForeground(TEXT_WHITE);
        ticketTextField.setCaretColor(TEXT_WHITE);
        ticketTextField.setFont(UITheme.fontBold(14));
        ticketTextField.setHorizontalAlignment(JTextField.CENTER);
        ticketTextField.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE, 1));

        final TicketInputFilter ticketFilter = new TicketInputFilter(90);
        ((AbstractDocument) ticketTextField.getDocument()).setDocumentFilter(ticketFilter);

        JButton minusBtn = new JButton("−");
        minusBtn.setPreferredSize(new Dimension(36, 36));
        minusBtn.setMargin(new Insets(0, 0, 0, 0));
        minusBtn.setBackground(CARD_BG);
        minusBtn.setForeground(TEXT_WHITE);
        minusBtn.setFont(UITheme.fontBold(16));
        minusBtn.setFocusPainted(false);
        minusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton plusBtn = new JButton("+");
        plusBtn.setPreferredSize(new Dimension(36, 36));
        plusBtn.setMargin(new Insets(0, 0, 0, 0));
        plusBtn.setBackground(CARD_BG);
        plusBtn.setForeground(TEXT_WHITE);
        plusBtn.setFont(UITheme.fontBold(16));
        plusBtn.setFocusPainted(false);
        plusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel ticketControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        ticketControlPanel.setOpaque(false);
        ticketControlPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        ticketControlPanel.add(minusBtn);
        ticketControlPanel.add(ticketTextField);
        ticketControlPanel.add(plusBtn);

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

        minusBtn.addActionListener(e -> {
            int cur = getValidTickets.get();
            if (cur > 1) ticketTextField.setText(String.valueOf(cur - 1));
        });

        plusBtn.addActionListener(e -> {
            int cur = getValidTickets.get();
            if (cur < ticketFilter.maxTickets) ticketTextField.setText(String.valueOf(cur + 1));
        });

        ticketTextField.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(ticketTextField::selectAll); }
            @Override public void focusLost(FocusEvent e) { getValidTickets.get(); }
        });

        ticketTextField.addActionListener(e -> getValidTickets.get());

        JCheckBox pwdCheckBox = new JCheckBox("PWD / Senior Citizen (20% Discount)");
        pwdCheckBox.setOpaque(false);
        pwdCheckBox.setForeground(TEXT_WHITE);
        pwdCheckBox.setFont(UITheme.fontRegular(12));
        pwdCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        pwdCheckBox.setSelected(initialIsPwd);

        String[] paymentOptions = {"GCash", "Maya", "Bank Account", "Cash / Over-the-Counter"};
        JComboBox<String> paymentCombo = new JComboBox<>(paymentOptions);
        paymentCombo.setPreferredSize(new Dimension(250, 36));
        paymentCombo.setMaximumSize(new Dimension(250, 36));
        paymentCombo.setBackground(PANEL_BG);
        paymentCombo.setForeground(TEXT_WHITE);
        paymentCombo.setFont(UITheme.fontRegular(13));
        paymentCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (initialPayment != null) paymentCombo.setSelectedItem(initialPayment);

        paymentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? ACCENT_PURPLE : PANEL_BG);
                lbl.setForeground(TEXT_WHITE);
                lbl.setBorder(new EmptyBorder(6, 10, 6, 10));
                return lbl;
            }
        });

        UITheme.GradientButton nextBtn = new UITheme.GradientButton("Proceed to Seat Selection", ACCENT_PURPLE, ACCENT_PINK, 14);
        nextBtn.setPreferredSize(new Dimension(260, 42));
        nextBtn.setMaximumSize(new Dimension(260, 42));
        nextBtn.setFont(UITheme.fontBold(14));
        nextBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        Runnable updateAvailability = () -> {
            LocalDate selectedDate = chosenDateHolder[0];
            int totalAvailable = getAvailableSeatCount(group.getTitle(), selectedDate, group.getTime());
            int unreservedPwd = getAvailablePwdSeatCount(group.getTitle(), selectedDate, group.getTime());
            boolean isPwd = pwdCheckBox.isSelected();

            int allowedMax = isPwd ? totalAvailable : (totalAvailable - unreservedPwd);

            if (allowedMax <= 0) {
                statusLabel.setText("Fully booked for " + selectedDate);
                statusLabel.setForeground(ACCENT_PINK);
                ticketTextField.setEnabled(false);
                minusBtn.setEnabled(false);
                plusBtn.setEnabled(false);
                nextBtn.setEnabled(false);
            } else {
                statusLabel.setText("Available Seats: " + allowedMax + " / 90");
                statusLabel.setForeground(ACCENT_GLOW);
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
        };

        pwdCheckBox.addActionListener(e -> updateAvailability.run());

        JPanel calendarPanel = createCustomCalendarPicker(chosenDateHolder, range[0], range[1], updateAvailability);
        updateAvailability.run();
        getValidTickets.get();

        nextBtn.addActionListener(e -> {
            int quantity = getValidTickets.get();
            LocalDate chosenDate = chosenDateHolder[0];
            String dateInput = chosenDate.toString();
            boolean hasDiscount = pwdCheckBox.isSelected();
            String paymentMethod = (String) paymentCombo.getSelectedItem();

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

        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 12)));
        panel.add(calendarPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(statusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createCenteredInputGroup("Number of Tickets:", ticketControlPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(pwdCheckBox);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createCenteredInputGroup("Payment Method:", paymentCombo));
        panel.add(Box.createRigidArea(new Dimension(0, 16)));
        panel.add(nextBtn);

        dialog.add(panel);
        dialog.setVisible(true);
    }

    private JPanel createCenteredInputGroup(String labelText, JComponent field) {
        JPanel group = new JPanel();
        group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));
        group.setOpaque(false);
        group.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel(labelText);
        label.setFont(UITheme.fontRegular(12));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        group.add(label);
        group.add(Box.createRigidArea(new Dimension(0, 4)));
        group.add(field);
        return group;
    }

    private JPanel createCustomCalendarPicker(LocalDate[] chosenDateHolder, LocalDate minDate, LocalDate maxDate, Runnable onDateSelected) {
        UITheme.RoundedPanel calContainer = new UITheme.RoundedPanel(16, PANEL_BG, UITheme.BORDER_SUBTLE, false);
        calContainer.setLayout(new BorderLayout(5, 8));
        calContainer.setBorder(new EmptyBorder(12, 12, 12, 12));
        calContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        final YearMonth[] currentYM = {YearMonth.from(chosenDateHolder[0])};

        JLabel monthYearHeader = new JLabel("", SwingConstants.CENTER);
        monthYearHeader.setFont(UITheme.fontBold(15));
        monthYearHeader.setForeground(TEXT_WHITE);

        JPanel gridPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        gridPanel.setOpaque(false);

        JButton prevBtn = new JButton("<");
        prevBtn.setFocusPainted(false);
        prevBtn.setBackground(CARD_BG);
        prevBtn.setForeground(TEXT_WHITE);

        JButton nextBtn = new JButton(">");
        nextBtn.setFocusPainted(false);
        nextBtn.setBackground(CARD_BG);
        nextBtn.setForeground(TEXT_WHITE);

        Runnable rebuildGrid = new Runnable() {
            @Override
            public void run() {
                gridPanel.removeAll();
                monthYearHeader.setText(currentYM[0].getMonth().toString() + " " + currentYM[0].getYear());

                String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                for (String d : days) {
                    JLabel dLbl = new JLabel(d, SwingConstants.CENTER);
                    dLbl.setFont(UITheme.fontBold(11));
                    dLbl.setForeground(TEXT_MUTED);
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
                    JButton dayBtn = new JButton(String.valueOf(day));
                    dayBtn.setFont(UITheme.fontBold(11));
                    dayBtn.setFocusPainted(false);
                    dayBtn.setMargin(new Insets(2, 2, 2, 2));

                    boolean isValidRange = !thisDate.isBefore(minDate) && !thisDate.isAfter(maxDate);

                    if (isValidRange) {
                        if (thisDate.equals(chosenDateHolder[0])) {
                            dayBtn.setBackground(ACCENT_PINK);
                            dayBtn.setForeground(TEXT_WHITE);
                        } else {
                            dayBtn.setBackground(CARD_BG);
                            dayBtn.setForeground(TEXT_WHITE);
                        }
                        dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        dayBtn.addActionListener(e -> {
                            chosenDateHolder[0] = thisDate;
                            run();
                            if (onDateSelected != null) onDateSelected.run();
                        });
                    } else {
                        dayBtn.setBackground(DISABLED_BG);
                        dayBtn.setForeground(TEXT_MUTED);
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

    private void showSeatPlanDialog(String movieTitle, LocalDate showDate, String showtime, MovieSchedule ticket, BillingCalculator billing, int requiredSeats, MovieGroup group, boolean isPwdSenior, String paymentMethod) {
        String cleanMovieTitle = movieTitle.replace("[CHRISTMAS PROMO]", "").trim();
        String seatWord = (requiredSeats == 1) ? "Seat" : "Seats";
        JDialog dialog = new JDialog(
                this,
                "Select " + requiredSeats + " " + seatWord + " - " + cleanMovieTitle + " | " + showDate + " | " + showtime,
                true
        );
        dialog.setSize(820, 640);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 22, 20, 22));

        UITheme.RoundedPanel screenBanner = new UITheme.RoundedPanel(12, CARD_BG, ACCENT_PURPLE, false);
        screenBanner.setLayout(new GridBagLayout());
        screenBanner.setPreferredSize(new Dimension(0, 38));
        JLabel screenLabel = new JLabel("CINEMA SCREEN");
        screenLabel.setForeground(ACCENT_GLOW);
        screenLabel.setFont(UITheme.fontBold(13));
        screenBanner.add(screenLabel);
        mainPanel.add(screenBanner, BorderLayout.NORTH);

        String seatKey = buildSeatPlanKey(movieTitle, showDate, showtime);
        char[][] seats = seatPlans.computeIfAbsent(seatKey, k -> createNewSeatPlan());
        List<Point> currentSelections = new ArrayList<>();

        JPanel gridPanel = new JPanel(new GridLayout(10, 9, 8, 8));
        gridPanel.setOpaque(false);

        char[] rowLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                char status = seats[r][c];
                String seatLabelText = rowLetters[r] + String.valueOf(c + 1);

                JButton seatBtn = new JButton(seatLabelText);
                seatBtn.setFont(UITheme.fontBold(11));
                seatBtn.setFocusPainted(false);

                if (status == 'X') {
                    seatBtn.setText("X");
                    seatBtn.setBackground(SEAT_RESERVED);
                    seatBtn.setForeground(TEXT_WHITE);
                    seatBtn.setEnabled(false);
                } else if (status == 'P') {
                    seatBtn.setText(seatLabelText + " (P)");
                    if (!isPwdSenior) {
                        seatBtn.setBackground(SEAT_PWD_DISABLED);
                        seatBtn.setForeground(TEXT_MUTED);
                        seatBtn.setEnabled(false);
                        seatBtn.setToolTipText("Reserved for PWD / Senior Citizens only");
                    } else {
                        seatBtn.setBackground(SEAT_PWD);
                        seatBtn.setForeground(TEXT_WHITE);
                    }
                } else {
                    seatBtn.setBackground(SEAT_AVAILABLE);
                    seatBtn.setForeground(TEXT_WHITE);
                }

                int finalR = r;
                int finalC = c;
                Point pt = new Point(finalR, finalC);

                seatBtn.addActionListener(e -> {
                    if (currentSelections.contains(pt)) {
                        currentSelections.remove(pt);
                        seatBtn.setBackground(seats[finalR][finalC] == 'P' ? SEAT_PWD : SEAT_AVAILABLE);
                        return;
                    }

                    if (currentSelections.size() >= requiredSeats) {
                        String limitSeatWord = (requiredSeats == 1) ? "seat" : "seats";
                        ThemedDialog.showInfo(dialog, "Limit Reached", "You can only select up to " + requiredSeats + " " + limitSeatWord + ".");
                        return;
                    }

                    currentSelections.add(pt);
                    seatBtn.setBackground(SEAT_SELECTED);
                });

                gridPanel.add(seatBtn);
            }
        }

        mainPanel.add(gridPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        JLabel legend = new JLabel("<html><font color='#37306B'>■</font> Available &nbsp;|&nbsp; <font color='#A855F7'>■</font> Selected &nbsp;|&nbsp; <font color='#B42828'>■</font> Reserved &nbsp;|&nbsp; <font color='#CA8A04'>■</font> PWD / Senior Only</html>");
        legend.setForeground(TEXT_WHITE);
        legend.setFont(UITheme.fontRegular(12));

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtnPanel.setOpaque(false);

        UITheme.GradientButton backBtn = UITheme.GradientButton.createGhost("Back", ACCENT_PURPLE, 12);
        backBtn.setPreferredSize(new Dimension(100, 38));
        backBtn.addActionListener(e -> {
            currentSelections.clear();
            dialog.dispose();
            startBookingFlow(group, showDate, requiredSeats, isPwdSenior, paymentMethod);
        });

        UITheme.GradientButton confirmBtn = new UITheme.GradientButton("Confirm Reservation", ACCENT_PURPLE, ACCENT_PINK, 12);
        confirmBtn.setPreferredSize(new Dimension(185, 38));
        confirmBtn.addActionListener(e -> {
            if (currentSelections.size() < requiredSeats) {
                String reqSeatWord = (requiredSeats == 1) ? "seat" : "seats";
                ThemedDialog.showError(dialog, "Seat Selection Incomplete", "Please select exactly " + requiredSeats + " " + reqSeatWord + " before confirming.");
                return;
            }

            if (!isPwdSenior) {
                for (Point pt : currentSelections) {
                    if (seats[pt.x][pt.y] == 'P') {
                        ThemedDialog.showError(dialog, "Invalid Seat Selection", "Yellow seats are strictly reserved for PWD / Senior Citizens!");
                        return;
                    }
                }
            }

            for (Point pt : currentSelections) {
                seats[pt.x][pt.y] = 'X';
            }

            dialog.dispose();
            showDoneDialog(ticket, billing, currentSelections, rowLetters, group);
        });

        actionBtnPanel.add(backBtn);
        actionBtnPanel.add(confirmBtn);

        bottomPanel.add(legend, BorderLayout.WEST);
        bottomPanel.add(actionBtnPanel, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        dialog.add(mainPanel);
        dialog.setVisible(true);
    }

    private void showDoneDialog(MovieSchedule ticket, BillingCalculator billing, List<Point> selectedSeats, char[] rowLetters, MovieGroup group) {
        JDialog dialog = new JDialog(this, "Reservation Complete", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(BG_DARK);
        mainContainer.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel confirmTitle = new JLabel("✓ Reservation Confirmed!");
        confirmTitle.setFont(UITheme.fontDisplay(26));
        confirmTitle.setForeground(ACCENT_GLOW);
        confirmTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtext = new JLabel("Please present this e-ticket receipt at the cinema counter.");
        subtext.setFont(UITheme.fontRegular(12));
        subtext.setForeground(TEXT_MUTED);
        subtext.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(UITheme.BORDER_SUBTLE);

        JPanel bodyPanel = new JPanel(new BorderLayout(16, 0));
        bodyPanel.setOpaque(false);

        UITheme.RoundedPanel posterContainer = new UITheme.RoundedPanel(16, PANEL_BG, ACCENT_PURPLE, false);
        posterContainer.setLayout(new GridBagLayout());
        posterContainer.setPreferredSize(new Dimension(190, 270));
        posterContainer.setMaximumSize(new Dimension(190, 270));

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 180, 260, 14);
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

        int randomNo = 100000 + (int) (Math.random() * 900000);
        JLabel resNoLabel = new JLabel("RESERVATION NO: SS-" + randomNo);
        resNoLabel.setFont(UITheme.fontBold(12));
        resNoLabel.setForeground(ACCENT_GLOW);

        boolean isPromoMovie = group.getTitle().contains("[CHRISTMAS PROMO]");
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        JLabel movieTitleLbl = new JLabel("<html><body style='width: 290px;'><b>" + cleanTitle + "</b></body></html>");
        movieTitleLbl.setFont(UITheme.fontBold(20));
        movieTitleLbl.setForeground(TEXT_WHITE);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        String formattedDate = ticket.getShowDate().format(dateFormatter);

        String ticketHeader = (selectedSeats.size() == 1) ? "TICKET" : "TICKETS";
        String ticketValueWord = (selectedSeats.size() == 1) ? "Ticket" : "Tickets";
        String seatLabel = (selectedSeats.size() == 1) ? "SEAT" : "SEATS";

        JPanel dateGroup = createReceiptDetailRow("DATE & TIME", formattedDate + " • " + ticket.getShowTime());
        JPanel ticketCountGroup = createReceiptDetailRow(ticketHeader, selectedSeats.size() + " " + ticketValueWord);
        JPanel paymentGroup = createReceiptDetailRow("PAYMENT METHOD", billing.getPaymentMethod());

        JPanel seatHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        seatHeaderPanel.setOpaque(false);
        JLabel seatLblHeader = new JLabel(seatLabel);
        seatLblHeader.setFont(UITheme.fontBold(11));
        seatLblHeader.setForeground(TEXT_MUTED);
        seatHeaderPanel.add(seatLblHeader);

        WrapLayoutContainer seatsChipContainer = new WrapLayoutContainer();
        for (Point p : selectedSeats) {
            String seatName = rowLetters[p.x] + String.valueOf(p.y + 1);
            UITheme.PillLabel chip = new UITheme.PillLabel(seatName, ACCENT_PURPLE, TEXT_WHITE, ACCENT_GLOW);
            seatsChipContainer.add(chip);
        }

        resNoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        movieTitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        dateGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        ticketCountGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        paymentGroup.setAlignmentX(Component.LEFT_ALIGNMENT);
        seatHeaderPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        seatHeaderPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 16));
        seatsChipContainer.setAlignmentX(Component.LEFT_ALIGNMENT);

        detailsPanel.add(resNoLabel);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        detailsPanel.add(movieTitleLbl);
        if (isPromoMovie) {
            UITheme.PillLabel promoBadge = new UITheme.PillLabel("CHRISTMAS PROMO", UITheme.PROMO_RED, TEXT_WHITE, UITheme.PROMO_GREEN);
            promoBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            detailsPanel.add(promoBadge);
        }
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 8)));
        detailsPanel.add(dateGroup);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        detailsPanel.add(ticketCountGroup);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        detailsPanel.add(paymentGroup);
        detailsPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        detailsPanel.add(seatHeaderPanel);
        detailsPanel.add(seatsChipContainer);

        if (billing.getDiscountAmount() > 0) {
            UITheme.PillLabel discountBadge = new UITheme.PillLabel("PWD / Senior – 20% OFF", new Color(22, 101, 52), new Color(134, 239, 172), UITheme.PROMO_GREEN);
            discountBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            detailsPanel.add(discountBadge);
        }

        bodyPanel.add(posterContainer, BorderLayout.WEST);
        bodyPanel.add(detailsPanel, BorderLayout.CENTER);

        UITheme.RoundedPanel priceBox = new UITheme.RoundedPanel(14, PANEL_BG, UITheme.BORDER_SUBTLE, false);
        priceBox.setLayout(new BoxLayout(priceBox, BoxLayout.Y_AXIS));
        priceBox.setBorder(new EmptyBorder(12, 16, 12, 16));
        priceBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));

        priceBox.add(createPriceRow("Price per Ticket", String.format("PHP %.2f", ticket.getPrice()), false, false));
        priceBox.add(createPriceRow("Subtotal (" + selectedSeats.size() + "x)", String.format("PHP %.2f", billing.getSubtotal()), false, false));

        if (billing.getDiscountAmount() > 0) {
            priceBox.add(createPriceRow("PWD/Senior Discount", String.format("-PHP %.2f", billing.getDiscountAmount()), true, false));
        }

        priceBox.add(createPriceRow("Online Service Fee", String.format("PHP %.2f", billing.getOnlineFee()), false, false));

        JSeparator priceSep = new JSeparator();
        priceSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        priceSep.setForeground(UITheme.BORDER_SUBTLE);
        priceBox.add(Box.createRigidArea(new Dimension(0, 5)));
        priceBox.add(priceSep);
        priceBox.add(Box.createRigidArea(new Dimension(0, 5)));

        priceBox.add(createPriceRow("TOTAL DUE", String.format("PHP %.2f", billing.getFinalTotal()), false, true));

        BarcodePanel barcodePanel = new BarcodePanel();
        barcodePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel footerText = new JLabel("Thank you for choosing SINE-SERVE!");
        footerText.setFont(UITheme.fontRegular(12));
        footerText.setForeground(TEXT_MUTED);
        footerText.setAlignmentX(Component.CENTER_ALIGNMENT);

        UITheme.GradientButton closeBtn = new UITheme.GradientButton("Back to Main Menu", ACCENT_PURPLE, ACCENT_PINK, 14);
        closeBtn.setPreferredSize(new Dimension(230, 42));
        closeBtn.setMaximumSize(new Dimension(230, 42));
        closeBtn.setFont(UITheme.fontBold(14));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dialog.dispose());

        mainContainer.add(confirmTitle);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 2)));
        mainContainer.add(subtext);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 10)));
        mainContainer.add(separator);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContainer.add(bodyPanel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContainer.add(priceBox);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContainer.add(barcodePanel);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 4)));
        mainContainer.add(footerText);
        mainContainer.add(Box.createRigidArea(new Dimension(0, 14)));
        mainContainer.add(closeBtn);

        dialog.add(mainContainer);
        dialog.pack();
        dialog.setSize(670, dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
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
        val.setFont(UITheme.fontBold(14));
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
        lbl.setFont(isTotal ? UITheme.fontBold(16) : UITheme.fontRegular(12));
        lbl.setForeground(isTotal ? TEXT_WHITE : TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(UITheme.fontBold(isTotal ? 18 : 13));

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
            setPreferredSize(new Dimension(160, 40));
            setMaximumSize(new Dimension(160, 40));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);

            int width = getWidth();
            int height = getHeight();
            int[] pattern = {2, 1, 3, 1, 4, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3};

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
        private static final int WRAP_WIDTH = 300;

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

        SwingUtilities.invokeLater(() -> new CreateAccount().setVisible(true));
    }
}