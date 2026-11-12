import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
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

    //Colors theme
    private static final Color BG_DARK = new Color(20, 14, 34);
    private static final Color PANEL_BG = new Color(32, 23, 53);
    private static final Color CARD_BG = new Color(44, 32, 70);
    private static final Color ACCENT_PURPLE = new Color(124, 58, 237);
    private static final Color ACCENT_RED = new Color(225, 29, 72);
    private static final Color ACCENT_LIGHT = new Color(168, 85, 247);
    private static final Color ACCENT_GLOW = new Color(192, 132, 252);
    private static final Color TRACK_BG = new Color(42, 31, 74);
    private static final Color TEXT_WHITE = new Color(243, 244, 246);
    private static final Color TEXT_MUTED = new Color(156, 163, 175);
    private static final Color DISABLED_BG = new Color(25, 20, 38);

    //Seats
    private static final Color SEAT_AVAILABLE = new Color(55, 48, 107);
    private static final Color SEAT_SELECTED = new Color(168, 85, 247);
    private static final Color SEAT_RESERVED = new Color(180, 40, 40);
    private static final Color SEAT_PWD = new Color(202, 138, 4);
    private static final Color SEAT_PWD_DISABLED = new Color(90, 65, 10);

    private final CinemaManager cinemaManager;
    private final Map<LocalDate, char[][]> dateSeatPlans;

    private static final Map<String, ImageIcon> posterCache = new HashMap<>();

    private JScrollPane carouselScrollPane;

    public CinemaUI() {
        this.cinemaManager = new CinemaManager();
        this.dateSeatPlans = new HashMap<>();

        setTitle("SINE-SERVE - Cinema Ticket Booking System");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1250, 880);
        setLocationRelativeTo(null);
        getContentPane().setBackground(BG_DARK);
        setLayout(new BorderLayout());

        verifyPosters();

        List<MovieGroup> allMovies = new ArrayList<>(cinemaManager.getRegularGroups());
        allMovies.addAll(cinemaManager.getPromoGroups());

        JPanel mainContent = new ScrollablePanel();
        mainContent.setLayout(new BoxLayout(mainContent, BoxLayout.Y_AXIS));
        mainContent.setBackground(BG_DARK);
        mainContent.setBorder(new EmptyBorder(15, 18, 20, 18));

        mainContent.add(createHeaderBanner());
        mainContent.add(Box.createRigidArea(new Dimension(0, 20)));
        mainContent.add(createTopBrowsingSection(allMovies));
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));
        mainContent.add(createMovieSectionTitle("AVAILABLE MOVIES FOR RESERVATION", null));
        mainContent.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContent.add(createMovieGridSection(cinemaManager.getRegularGroups()));
        mainContent.add(Box.createRigidArea(new Dimension(0, 25)));
        mainContent.add(createMovieSectionTitle("CHRISTMAS HOLIDAY PROMOS", "(PHP 102.00)"));
        mainContent.add(Box.createRigidArea(new Dimension(0, 12)));
        mainContent.add(createMovieGridSection(cinemaManager.getPromoGroups()));

        JScrollPane pageScrollPane = new JScrollPane(mainContent);
        pageScrollPane.setBorder(null);
        pageScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        pageScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(pageScrollPane, BorderLayout.CENTER);
    }

    private File findImageFile(String path) {
        if (path == null || path.trim().isEmpty()) return null;
        
        File file = new File(path);
        if (file.exists()) return file;

        File userDirFile = new File(System.getProperty("user.dir"), path);
        if (userDirFile.exists()) return userDirFile;

        return null;
    }

    private ImageIcon loadPoster(String path, int targetWidth, int targetHeight) {
        if (path == null || path.trim().isEmpty()) return null;

        String cacheKey = path + "_" + targetWidth + "x" + targetHeight;
        if (posterCache.containsKey(cacheKey)) {
            return posterCache.get(cacheKey);
        }

        File imgFile = findImageFile(path);
        if (imgFile == null) {
            System.out.println("Poster not found: " + new File(path).getAbsolutePath());
            return null;
        }

        try {
            BufferedImage originalImg = ImageIO.read(imgFile);
            if (originalImg == null) {
                System.out.println("Poster load error (null): " + imgFile.getAbsolutePath());
                return null;
            }

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

            if (origW < targetWidth || origH < targetHeight) {
                System.out.println("WARNING low-res poster (" + origW + "x" + origH + "): " + imgFile.getName());
            }

            ImageIcon icon = new ImageIcon(croppedImg);
            posterCache.put(cacheKey, icon);
            return icon;

        } catch (Exception e) {
            System.out.println("Poster exception: " + imgFile.getAbsolutePath() + " - " + e.getMessage());
            return null;
        }
    }

    private void verifyPosters() {
        System.out.println("\n========== POSTER FILE STARTUP CHECK ==========");
        List<MovieGroup> allMovies = new ArrayList<>(cinemaManager.getRegularGroups());
        allMovies.addAll(cinemaManager.getPromoGroups());

        for (MovieGroup m : allMovies) {
            String path = m.getPosterPath();
            File found = findImageFile(path);

            if (found != null && found.exists()) {
                System.out.println("OK: " + m.getTitle() + " -> " + path);
            } else {
                File abs = new File(path != null ? path : "null");
                System.out.println("MISSING: " + m.getTitle() + " -> " + abs.getAbsolutePath());
            }
        }
        System.out.println("================================================\n");
    }

    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) { return 16; }
        @Override public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) { return 50; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; } 
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    private JPanel createHeaderBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(PANEL_BG);
        banner.setBorder(new EmptyBorder(12, 20, 12, 20));
        banner.setPreferredSize(new Dimension(0, 90));
        banner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));
        banner.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel leftBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        leftBox.setOpaque(false);

        JLabel logo = new JLabel("S I N E - S E R V E");
        logo.setFont(new Font("SansSerif", Font.BOLD, 42));
        logo.setForeground(ACCENT_GLOW);

        JLabel divider = new JLabel("|");
        divider.setFont(new Font("SansSerif", Font.PLAIN, 28));
        divider.setForeground(ACCENT_PURPLE);

        JLabel tag = new JLabel("BOOK CINEMA TICKET NOW!");
        tag.setFont(new Font("SansSerif", Font.BOLD, 15));
        tag.setForeground(TEXT_WHITE);

        leftBox.add(logo);
        leftBox.add(divider);
        leftBox.add(tag);

        JLabel subtitle = new JLabel("SOBRANG GWAPO NUNG GUMAWA");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_MUTED); 

        banner.add(leftBox, BorderLayout.WEST);
        banner.add(subtitle, BorderLayout.EAST);
        return banner;
    }

    private JPanel createTopBrowsingSection(List<MovieGroup> movies) {
        JPanel container = new JPanel(new BorderLayout(0, 10));
        container.setOpaque(false);
        container.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titleWithBar = createMovieSectionTitle("FEATURED MOVIE SHOWCASE", "(Browse Only)");
        
        JLabel hintLabel = new JLabel("Scroll to see more ➔ ");
        hintLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
        hintLabel.setForeground(ACCENT_LIGHT);

        headerPanel.add(titleWithBar, BorderLayout.WEST);
        headerPanel.add(hintLabel, BorderLayout.EAST);
        container.add(headerPanel, BorderLayout.NORTH);

        JPanel carouselGrid = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 5));
        carouselGrid.setOpaque(false);

        for (MovieGroup group : movies) {
            carouselGrid.add(createDisplayOnlyCard(group));
        }

        carouselScrollPane = new JScrollPane(carouselGrid);
        carouselScrollPane.setBorder(null);
        carouselScrollPane.setOpaque(false);
        carouselScrollPane.getViewport().setOpaque(false);
        carouselScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        carouselScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        carouselScrollPane.setPreferredSize(new Dimension(0, 365));

        JScrollBar hBar = carouselScrollPane.getHorizontalScrollBar();
        hBar.setPreferredSize(new Dimension(0, 16));
        hBar.setUI(new CustomPurpleScrollBarUI());

        MouseAdapter scrollAdapter = new MouseAdapter() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                JScrollBar hScrollBar = carouselScrollPane.getHorizontalScrollBar();
                int scrollAmount = e.getUnitsToScroll() * 20;
                hScrollBar.setValue(hScrollBar.getValue() + scrollAmount);
            }
        };

        carouselScrollPane.addMouseWheelListener(scrollAdapter);
        carouselGrid.addMouseWheelListener(scrollAdapter);

        container.add(carouselScrollPane, BorderLayout.CENTER);
        return container;
    }

    private static class CustomPurpleScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = ACCENT_LIGHT;
            this.trackColor = TRACK_BG;
        }

        @Override
        protected JButton createDecreaseButton(int orientation) { return createZeroButton(); }
        @Override
        protected JButton createIncreaseButton(int orientation) { return createZeroButton(); }

        private JButton createZeroButton() {
            JButton btn = new JButton();
            btn.setPreferredSize(new Dimension(0, 0));
            return btn;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TRACK_BG);
            g2.fillRoundRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height, 12, 12);
            g2.setColor(ACCENT_PURPLE);
            g2.drawRoundRect(trackBounds.x, trackBounds.y, trackBounds.width - 1, trackBounds.height - 1, 12, 12);
            g2.dispose();
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {
            if (thumbBounds.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Color currentThumbColor = isDragging ? ACCENT_GLOW : (isThumbRollover() ? ACCENT_LIGHT : ACCENT_PURPLE);
            g2.setColor(currentThumbColor);
            g2.fillRoundRect(thumbBounds.x + 2, thumbBounds.y + 2, thumbBounds.width - 4, thumbBounds.height - 4, 10, 10);
            g2.dispose();
        }
    }

    private JPanel createDisplayOnlyCard(MovieGroup group) {
        JPanel card = new JPanel();
        card.setPreferredSize(new Dimension(240, 330));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(PANEL_BG, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));

        JPanel posterBox = new JPanel(new GridBagLayout());
        posterBox.setPreferredSize(new Dimension(215, 180));
        posterBox.setMaximumSize(new Dimension(215, 180));
        posterBox.setBackground(PANEL_BG);

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 215, 180);

        if (posterIcon != null) {
            posterLabel = new JLabel(posterIcon);
        } else {
            posterLabel = new JLabel("<html><center><b>SINE-SERVE</b><br><br><font color='#F3F4F6' size='4'>" + group.getTitle() + "</font></center></html>");
            posterLabel.setForeground(TEXT_MUTED);
            posterLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        }

        posterBox.add(posterLabel);

        JLabel titleLbl = new JLabel("<html><body style='width: 190px;'><center><b>" + group.getTitle() + "</b></center></body></html>");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 15));
        titleLbl.setForeground(TEXT_WHITE);
        titleLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel dateLbl = new JLabel("Date: " + group.getDateRange());
        dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        dateLbl.setForeground(TEXT_MUTED);
        dateLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel timeLbl = new JLabel("Showtime: " + group.getTime());
        timeLbl.setFont(new Font("SansSerif", Font.PLAIN, 12));
        timeLbl.setForeground(TEXT_MUTED);
        timeLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel priceLbl = new JLabel("PHP " + String.format("%.2f", group.getPrice()));
        priceLbl.setForeground(ACCENT_LIGHT);
        priceLbl.setFont(new Font("SansSerif", Font.BOLD, 14));
        priceLbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        card.add(posterBox);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(titleLbl);
        card.add(Box.createRigidArea(new Dimension(0, 4)));
        card.add(dateLbl);
        card.add(Box.createRigidArea(new Dimension(0, 2)));
        card.add(timeLbl);
        card.add(Box.createRigidArea(new Dimension(0, 6)));
        card.add(priceLbl);

        return card;
    }

    private JPanel createMovieSectionTitle(String mainTitle, String subTitle) {
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        titlePanel.setOpaque(false);
        titlePanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel barAccent = new JPanel();
        barAccent.setPreferredSize(new Dimension(5, 30));
        barAccent.setBackground(ACCENT_LIGHT);

        JLabel mainLabel = new JLabel(mainTitle);
        mainLabel.setFont(new Font("SansSerif", Font.BOLD, 26));
        mainLabel.setForeground(TEXT_WHITE);

        titlePanel.add(barAccent);
        titlePanel.add(mainLabel);

        if (subTitle != null && !subTitle.isEmpty()) {
            JLabel subLabel = new JLabel(subTitle);
            subLabel.setFont(new Font("SansSerif", Font.PLAIN, 16));
            subLabel.setForeground(TEXT_MUTED);
            titlePanel.add(subLabel);
        }

        return titlePanel;
    }

    private JPanel createMovieGridSection(List<MovieGroup> groups) {
        JPanel grid = new JPanel(new GridLayout(0, 3, 8, 8));
        grid.setOpaque(false);
        grid.setAlignmentX(Component.LEFT_ALIGNMENT);

        for (MovieGroup group : groups) {
            grid.add(createMovieBoxCard(group));
        }

        return grid;
    }

    private JPanel createMovieBoxCard(MovieGroup group) {
        JPanel card = new JPanel(new BorderLayout(12, 0));
        card.setBackground(PANEL_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BG, 1),
            new EmptyBorder(10, 10, 10, 10)
        ));
        card.setPreferredSize(new Dimension(0, 260));

        JPanel posterBox = new JPanel(new GridBagLayout());
        posterBox.setPreferredSize(new Dimension(160, 240));
        posterBox.setMinimumSize(new Dimension(160, 240));
        posterBox.setBackground(CARD_BG);

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 160, 240);

        if (posterIcon != null) {
            posterLabel = new JLabel(posterIcon);
        } else {
            posterLabel = new JLabel("<html><center><b>SINE-SERVE</b><br><br><font color='#F3F4F6' size='4'>" + group.getTitle() + "</font></center></html>");
            posterLabel.setForeground(TEXT_MUTED);
            posterLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        }
        posterBox.add(posterLabel);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        JLabel titleLbl = new JLabel("<html><body style='width: 170px;'><b>" + group.getTitle() + "</b></body></html>");
        titleLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        titleLbl.setForeground(TEXT_WHITE);
        titleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel dateLbl = new JLabel(group.getDateRange());
        dateLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        dateLbl.setForeground(TEXT_MUTED);
        dateLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel timeLbl = new JLabel("Showtime: " + group.getTime());
        timeLbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        timeLbl.setForeground(TEXT_MUTED);
        timeLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel priceLbl = new JLabel("PHP " + String.format("%.2f", group.getPrice()));
        priceLbl.setFont(new Font("SansSerif", Font.BOLD, 18));
        priceLbl.setForeground(ACCENT_LIGHT);
        priceLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton reserveBtn = new JButton("RESERVE");
        reserveBtn.setPreferredSize(new Dimension(140, 42));
        reserveBtn.setMaximumSize(new Dimension(140, 42));
        reserveBtn.setBackground(ACCENT_PURPLE);
        reserveBtn.setForeground(TEXT_WHITE);
        reserveBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        reserveBtn.setFocusPainted(false);
        reserveBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        reserveBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        reserveBtn.addActionListener(e -> startBookingFlow(group, null, 1, false, "GCash"));

        infoPanel.add(titleLbl);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 6)));
        infoPanel.add(dateLbl);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));
        infoPanel.add(timeLbl);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        infoPanel.add(priceLbl);
        infoPanel.add(Box.createVerticalGlue());
        infoPanel.add(reserveBtn);
        infoPanel.add(Box.createRigidArea(new Dimension(0, 4)));

        card.add(posterBox, BorderLayout.WEST);
        card.add(infoPanel, BorderLayout.CENTER);

        return card;
    }

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

    private int getAvailableSeatCount(LocalDate date) {
        char[][] seats = dateSeatPlans.computeIfAbsent(date, k -> createNewSeatPlan());
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

    private int getAvailablePwdSeatCount(LocalDate date) {
        char[][] seats = dateSeatPlans.computeIfAbsent(date, k -> createNewSeatPlan());
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

    //TICKET INPUT FILTER
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

    //STEP 1 BOOKING FLOW DIALOG
    private void startBookingFlow(MovieGroup group, LocalDate initialDate, int initialQuantity, boolean initialIsPwd, String initialPayment) {
        JDialog dialog = new JDialog(this, "Select Date & Booking Details", true);
        dialog.setSize(520, 660);
        dialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_DARK);
        panel.setBorder(new EmptyBorder(20, 25, 20, 25));

        JLabel title = new JLabel("Select Date for " + group.getTitle());
        title.setFont(new Font("SansSerif", Font.BOLD, 18));
        title.setForeground(TEXT_WHITE);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        LocalDate[] range = parseMovieDateRange(group.getDateRange());
        final LocalDate[] chosenDateHolder = {initialDate != null ? initialDate : range[0]};

        JLabel statusLabel = new JLabel("", SwingConstants.CENTER);
        statusLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JTextField ticketTextField = new JTextField(String.valueOf(initialQuantity));
        ticketTextField.setPreferredSize(new Dimension(80, 34));
        ticketTextField.setMaximumSize(new Dimension(80, 34));
        ticketTextField.setBackground(PANEL_BG);
        ticketTextField.setForeground(TEXT_WHITE);
        ticketTextField.setCaretColor(TEXT_WHITE);
        ticketTextField.setFont(new Font("SansSerif", Font.BOLD, 14));
        ticketTextField.setHorizontalAlignment(JTextField.CENTER);
        ticketTextField.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE, 1));

        final TicketInputFilter ticketFilter = new TicketInputFilter(90);
        ((AbstractDocument) ticketTextField.getDocument()).setDocumentFilter(ticketFilter);

        JButton minusBtn = new JButton("−");
        minusBtn.setPreferredSize(new Dimension(34, 34));
        minusBtn.setMargin(new Insets(0, 0, 0, 0));
        minusBtn.setBackground(CARD_BG);
        minusBtn.setForeground(TEXT_WHITE);
        minusBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        minusBtn.setFocusPainted(false);
        minusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JButton plusBtn = new JButton("+");
        plusBtn.setPreferredSize(new Dimension(34, 34));
        plusBtn.setMargin(new Insets(0, 0, 0, 0));
        plusBtn.setBackground(CARD_BG);
        plusBtn.setForeground(TEXT_WHITE);
        plusBtn.setFont(new Font("SansSerif", Font.BOLD, 16));
        plusBtn.setFocusPainted(false);
        plusBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JPanel ticketControlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
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
            if (cur > 1) {
                ticketTextField.setText(String.valueOf(cur - 1));
            }
        });

        plusBtn.addActionListener(e -> {
            int cur = getValidTickets.get();
            if (cur < ticketFilter.maxTickets) {
                ticketTextField.setText(String.valueOf(cur + 1));
            }
        });

        ticketTextField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(ticketTextField::selectAll);
            }

            @Override
            public void focusLost(FocusEvent e) {
                getValidTickets.get();
            }
        });

        ticketTextField.addActionListener(e -> getValidTickets.get());

        JCheckBox pwdCheckBox = new JCheckBox("PWD / Senior Citizen (20% Discount)");
        pwdCheckBox.setOpaque(false);
        pwdCheckBox.setForeground(TEXT_WHITE);
        pwdCheckBox.setFont(new Font("SansSerif", Font.PLAIN, 12));
        pwdCheckBox.setAlignmentX(Component.CENTER_ALIGNMENT);
        pwdCheckBox.setSelected(initialIsPwd);

        String[] paymentOptions = {"GCash", "Maya", "Bank Account", "Cash / Over-the-Counter"};
        JComboBox<String> paymentCombo = new JComboBox<>(paymentOptions);
        paymentCombo.setPreferredSize(new Dimension(240, 34));
        paymentCombo.setMaximumSize(new Dimension(240, 34));
        paymentCombo.setBackground(PANEL_BG);
        paymentCombo.setForeground(TEXT_WHITE);
        paymentCombo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        paymentCombo.setAlignmentX(Component.CENTER_ALIGNMENT);
        if (initialPayment != null) {
            paymentCombo.setSelectedItem(initialPayment);
        }
        
        paymentCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setOpaque(true);
                lbl.setBackground(isSelected ? ACCENT_PURPLE : PANEL_BG);
                lbl.setForeground(TEXT_WHITE);
                lbl.setBorder(new EmptyBorder(5, 8, 5, 8));
                return lbl;
            }
        });

        JButton nextBtn = new JButton("Proceed to Seat Selection");
        nextBtn.setPreferredSize(new Dimension(240, 40));
        nextBtn.setMaximumSize(new Dimension(240, 40));
        nextBtn.setBackground(ACCENT_RED);
        nextBtn.setForeground(TEXT_WHITE);
        nextBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        nextBtn.setFocusPainted(false);
        nextBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        nextBtn.setAlignmentX(Component.CENTER_ALIGNMENT);

        Runnable updateAvailability = () -> {
            LocalDate selectedDate = chosenDateHolder[0];
            int totalAvailable = getAvailableSeatCount(selectedDate);
            int unreservedPwd = getAvailablePwdSeatCount(selectedDate);
            boolean isPwd = pwdCheckBox.isSelected();

            int allowedMax = isPwd ? totalAvailable : (totalAvailable - unreservedPwd);

            if (allowedMax <= 0) {
                statusLabel.setText("Fully booked for " + selectedDate);
                statusLabel.setForeground(ACCENT_RED);
                ticketTextField.setEnabled(false);
                minusBtn.setEnabled(false);
                plusBtn.setEnabled(false);
                nextBtn.setEnabled(false);
            } else {
                statusLabel.setText("Available Seats: " + allowedMax + " / 90");
                statusLabel.setForeground(ACCENT_LIGHT);
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
                showSeatPlanDialog(validDate, ticket, billing, quantity, group, hasDiscount, paymentMethod);

            } catch (InvalidDateException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Invalid Date", JOptionPane.ERROR_MESSAGE);
            } catch (ScheduleNotFoundException ex) {
                JOptionPane.showMessageDialog(dialog, ex.getMessage(), "Schedule Not Found", JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "The selected date is outside this movie's schedule range!", "Schedule Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        panel.add(title);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(calendarPanel);
        panel.add(Box.createRigidArea(new Dimension(0, 8)));
        panel.add(statusLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createCenteredInputGroup("Number of Tickets:", ticketControlPanel));
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(pwdCheckBox);
        panel.add(Box.createRigidArea(new Dimension(0, 10)));
        panel.add(createCenteredInputGroup("Payment Method:", paymentCombo));
        panel.add(Box.createRigidArea(new Dimension(0, 15)));
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
        label.setFont(new Font("SansSerif", Font.PLAIN, 12));
        label.setForeground(TEXT_MUTED);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        group.add(label);
        group.add(Box.createRigidArea(new Dimension(0, 4)));
        group.add(field);
        return group;
    }

    private JPanel createCustomCalendarPicker(LocalDate[] chosenDateHolder, LocalDate minDate, LocalDate maxDate, Runnable onDateSelected) {
        JPanel calContainer = new JPanel(new BorderLayout(5, 5));
        calContainer.setBackground(PANEL_BG);
        calContainer.setBorder(new EmptyBorder(12, 12, 12, 12));
        calContainer.setAlignmentX(Component.CENTER_ALIGNMENT);

        final YearMonth[] currentYM = {YearMonth.from(chosenDateHolder[0])};

        JLabel monthYearHeader = new JLabel("", SwingConstants.CENTER);
        monthYearHeader.setFont(new Font("SansSerif", Font.BOLD, 15));
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
                    dLbl.setFont(new Font("SansSerif", Font.BOLD, 11));
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
                    dayBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
                    dayBtn.setFocusPainted(false);
                    dayBtn.setMargin(new Insets(2, 2, 2, 2));

                    boolean isValidRange = !thisDate.isBefore(minDate) && !thisDate.isAfter(maxDate);

                    if (isValidRange) {
                        if (thisDate.equals(chosenDateHolder[0])) {
                            dayBtn.setBackground(ACCENT_RED);
                            dayBtn.setForeground(TEXT_WHITE);
                        } else {
                            dayBtn.setBackground(CARD_BG);
                            dayBtn.setForeground(TEXT_WHITE);
                        }
                        dayBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
                        dayBtn.addActionListener(e -> {
                            chosenDateHolder[0] = thisDate;
                            run();
                            if (onDateSelected != null) {
                                onDateSelected.run();
                            }
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

    //STEP 2 SEAT SELECTION DIALOG
    private void showSeatPlanDialog(LocalDate showDate, MovieSchedule ticket, BillingCalculator billing, int requiredSeats, MovieGroup group, boolean isPwdSenior, String paymentMethod) {
        JDialog dialog = new JDialog(this, "Select " + requiredSeats + " Seat(s) for " + showDate, true);
        dialog.setSize(800, 620);
        dialog.setLocationRelativeTo(this);

        JPanel mainPanel = new JPanel(new BorderLayout(10, 15));
        mainPanel.setBackground(BG_DARK);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel screenLabel = new JLabel("--- CINEMA SCREEN ---", SwingConstants.CENTER);
        screenLabel.setOpaque(true);
        screenLabel.setBackground(CARD_BG);
        screenLabel.setForeground(TEXT_MUTED);
        screenLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        screenLabel.setPreferredSize(new Dimension(0, 35));
        mainPanel.add(screenLabel, BorderLayout.NORTH);

        char[][] seats = dateSeatPlans.computeIfAbsent(showDate, k -> createNewSeatPlan());
        List<Point> currentSelections = new ArrayList<>();

        JPanel gridPanel = new JPanel(new GridLayout(10, 9, 8, 8));
        gridPanel.setOpaque(false);

        char[] rowLetters = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'};

        for (int r = 0; r < 10; r++) {
            for (int c = 0; c < 9; c++) {
                char status = seats[r][c];
                String seatLabelText = rowLetters[r] + String.valueOf(c + 1);

                JButton seatBtn = new JButton(seatLabelText);
                seatBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
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
                        JOptionPane.showMessageDialog(dialog, 
                            "You can only select up to " + requiredSeats + " seat(s).", 
                            "Limit Reached", 
                            JOptionPane.INFORMATION_MESSAGE);
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

        JLabel legend = new JLabel("<html><font color='#37306B'>■</font> Available | <font color='#A855F7'>■</font> Selected | <font color='#B42828'>■</font> Reserved | <font color='#CA8A04'>■</font> PWD / Senior Only</html>");
        legend.setForeground(TEXT_WHITE);

        JPanel actionBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBtnPanel.setOpaque(false);

        JButton backBtn = new JButton("Back");
        backBtn.setPreferredSize(new Dimension(100, 36));
        backBtn.setBackground(BG_DARK);
        backBtn.setForeground(TEXT_WHITE);
        backBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        backBtn.setFocusPainted(false);
        backBtn.setBorder(BorderFactory.createLineBorder(ACCENT_PURPLE, 2));
        backBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        backBtn.addActionListener(e -> {
            currentSelections.clear();
            dialog.dispose();
            startBookingFlow(group, showDate, requiredSeats, isPwdSenior, paymentMethod);
        });

        JButton confirmBtn = new JButton("Confirm Reservation");
        confirmBtn.setPreferredSize(new Dimension(170, 36));
        confirmBtn.setBackground(ACCENT_PURPLE);
        confirmBtn.setForeground(TEXT_WHITE);
        confirmBtn.setFont(new Font("SansSerif", Font.BOLD, 13));
        confirmBtn.setFocusPainted(false);
        confirmBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        confirmBtn.addActionListener(e -> {
            if (currentSelections.size() < requiredSeats) {
                JOptionPane.showMessageDialog(dialog, "Please select exactly " + requiredSeats + " seat(s) before confirming.", "Seat Selection Incomplete", JOptionPane.WARNING_MESSAGE);
                return;
            }

            if (!isPwdSenior) {
                for (Point pt : currentSelections) {
                    if (seats[pt.x][pt.y] == 'P') {
                        JOptionPane.showMessageDialog(dialog, "Yellow seats are strictly reserved for PWD / Senior Citizens!", "Invalid Seat Selection", JOptionPane.ERROR_MESSAGE);
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

    //STEP 3 RESERVATION COMPLETE DIALOG
    private void showDoneDialog(MovieSchedule ticket, BillingCalculator billing, List<Point> selectedSeats, char[] rowLetters, MovieGroup group) {
        JDialog dialog = new JDialog(this, "Reservation Complete", true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel mainContainer = new JPanel();
        mainContainer.setLayout(new BoxLayout(mainContainer, BoxLayout.Y_AXIS));
        mainContainer.setBackground(BG_DARK);
        mainContainer.setBorder(new EmptyBorder(18, 22, 18, 22));

        //HEADER
        JLabel confirmTitle = new JLabel("✓ Reservation Confirmed!");
        confirmTitle.setFont(new Font("SansSerif", Font.BOLD, 28));
        confirmTitle.setForeground(ACCENT_LIGHT);
        confirmTitle.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtext = new JLabel("Please present this receipt at the cinema counter.");
        subtext.setFont(new Font("SansSerif", Font.PLAIN, 12));
        subtext.setForeground(TEXT_MUTED);
        subtext.setAlignmentX(Component.CENTER_ALIGNMENT);

        JSeparator separator = new JSeparator();
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(ACCENT_PURPLE);
        separator.setBackground(ACCENT_PURPLE);

        //RECEIPT BODY
        JPanel bodyPanel = new JPanel(new BorderLayout(15, 0));
        bodyPanel.setOpaque(false);

        //MOVIE POSTER
        JPanel posterContainer = new JPanel(new GridBagLayout());
        posterContainer.setPreferredSize(new Dimension(190, 270));
        posterContainer.setMaximumSize(new Dimension(190, 270));
        posterContainer.setBackground(PANEL_BG);
        posterContainer.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ACCENT_PURPLE, 1, true),
            new EmptyBorder(4, 4, 4, 4)
        ));

        JLabel posterLabel;
        ImageIcon posterIcon = loadPoster(group.getPosterPath(), 180, 260);

        if (posterIcon != null) {
            posterLabel = new JLabel(posterIcon);
        } else {
            posterLabel = new JLabel("<html><center><b>SINE-SERVE</b><br><br><font color='#F3F4F6' size='4'>" + group.getTitle() + "</font></center></html>");
            posterLabel.setForeground(TEXT_MUTED);
            posterLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
        }
        posterContainer.add(posterLabel);

        //DETAILS PANEL
        JPanel detailsPanel = new JPanel();
        detailsPanel.setLayout(new BoxLayout(detailsPanel, BoxLayout.Y_AXIS));
        detailsPanel.setOpaque(false);

        int randomNo = 100000 + (int)(Math.random() * 900000);
        JLabel resNoLabel = new JLabel("RESERVATION NO: SS-" + randomNo);
        resNoLabel.setFont(new Font("SansSerif", Font.BOLD, 12));
        resNoLabel.setForeground(ACCENT_GLOW);

        boolean isPromoMovie = group.getTitle().contains("[CHRISTMAS PROMO]");
        String cleanTitle = group.getTitle().replace("[CHRISTMAS PROMO]", "").trim();
        JLabel movieTitleLbl = new JLabel("<html><body style='width: 290px;'><b>" + cleanTitle + "</b></body></html>");
        movieTitleLbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        movieTitleLbl.setForeground(TEXT_WHITE);

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy");
        String formattedDate = ticket.getShowDate().format(dateFormatter);

        JPanel dateGroup = createReceiptDetailRow("DATE & TIME", formattedDate + " • " + ticket.getShowTime());
        JPanel ticketCountGroup = createReceiptDetailRow("TICKETS", selectedSeats.size() + " Ticket(s)");
        JPanel paymentGroup = createReceiptDetailRow("PAYMENT METHOD", billing.getPaymentMethod());

        JPanel seatHeaderPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        seatHeaderPanel.setOpaque(false);
        JLabel seatLblHeader = new JLabel("SEATS");
        seatLblHeader.setFont(new Font("SansSerif", Font.BOLD, 11));
        seatLblHeader.setForeground(TEXT_MUTED);
        seatHeaderPanel.add(seatLblHeader);

        WrapLayoutContainer seatsChipContainer = new WrapLayoutContainer();
        for (Point p : selectedSeats) {
            String seatName = rowLetters[p.x] + String.valueOf(p.y + 1);
            JLabel chip = new JLabel(seatName);
            chip.setOpaque(true);
            chip.setBackground(ACCENT_PURPLE);
            chip.setForeground(TEXT_WHITE);
            chip.setFont(new Font("SansSerif", Font.BOLD, 11));
            chip.setBorder(new EmptyBorder(3, 8, 3, 8));
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
            JLabel promoBadge = new JLabel(" CHRISTMAS PROMO ");
            promoBadge.setOpaque(true);
            promoBadge.setBackground(new Color(153, 27, 27));
            promoBadge.setForeground(new Color(254, 202, 202));
            promoBadge.setFont(new Font("SansSerif", Font.BOLD, 10));
            promoBadge.setBorder(new EmptyBorder(2, 6, 2, 6));
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
            JLabel discountBadge = new JLabel(" PWD / Senior – 20% OFF ");
            discountBadge.setOpaque(true);
            discountBadge.setBackground(new Color(22, 101, 52));
            discountBadge.setForeground(new Color(134, 239, 172));
            discountBadge.setFont(new Font("SansSerif", Font.BOLD, 11));
            discountBadge.setBorder(new EmptyBorder(3, 6, 3, 6));
            discountBadge.setAlignmentX(Component.LEFT_ALIGNMENT);
            detailsPanel.add(Box.createRigidArea(new Dimension(0, 4)));
            detailsPanel.add(discountBadge);
        }

        bodyPanel.add(posterContainer, BorderLayout.WEST);
        bodyPanel.add(detailsPanel, BorderLayout.CENTER);

        //PRICE BREAKDOWN PANEL
        JPanel priceBox = new JPanel();
        priceBox.setLayout(new BoxLayout(priceBox, BoxLayout.Y_AXIS));
        priceBox.setBackground(PANEL_BG);
        priceBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(CARD_BG, 1),
            new EmptyBorder(10, 15, 10, 15)
        ));
        priceBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        priceBox.add(createPriceRow("Price per Ticket", String.format("PHP %.2f", ticket.getPrice()), false, false));
        priceBox.add(createPriceRow("Subtotal (" + selectedSeats.size() + "x)", String.format("PHP %.2f", billing.getSubtotal()), false, false));

        if (billing.getDiscountAmount() > 0) {
            priceBox.add(createPriceRow("PWD/Senior Discount", String.format("-PHP %.2f", billing.getDiscountAmount()), true, false));
        }

        priceBox.add(createPriceRow("Online Service Fee", String.format("PHP %.2f", billing.getOnlineFee()), false, false));
        
        JSeparator priceSep = new JSeparator();
        priceSep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        priceSep.setForeground(TEXT_MUTED);
        priceBox.add(Box.createRigidArea(new Dimension(0, 4)));
        priceBox.add(priceSep);
        priceBox.add(Box.createRigidArea(new Dimension(0, 4)));

        priceBox.add(createPriceRow("TOTAL DUE", String.format("PHP %.2f", billing.getFinalTotal()), false, true));

        //BARCODE & FOOTER
        BarcodePanel barcodePanel = new BarcodePanel();
        barcodePanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel footerText = new JLabel("Thank you for choosing SINE-SERVE!");
        footerText.setFont(new Font("SansSerif", Font.ITALIC, 12));
        footerText.setForeground(TEXT_MUTED);
        footerText.setAlignmentX(Component.CENTER_ALIGNMENT);

        //MAIN ACTION BUTTON
        JButton closeBtn = new JButton("Back to Main Menu");
        closeBtn.setPreferredSize(new Dimension(220, 42));
        closeBtn.setMaximumSize(new Dimension(220, 42));
        closeBtn.setBackground(ACCENT_PURPLE);
        closeBtn.setForeground(TEXT_WHITE);
        closeBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        closeBtn.setFocusPainted(false);
        closeBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        closeBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        closeBtn.addActionListener(e -> dialog.dispose());

        //ASSEMBLY
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
        dialog.setSize(660, dialog.getHeight());
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    //HELPER METHOD FOR RECEIPT DETAILS
    private JPanel createReceiptDetailRow(String label, String value) {
        JPanel row = new JPanel();
        row.setLayout(new BoxLayout(row, BoxLayout.Y_AXIS));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 10));
        lbl.setForeground(TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, 14));
        val.setForeground(TEXT_WHITE);

        row.add(lbl);
        row.add(val);
        return row;
    }

    //HELPER METHOD FOR PRICE BREAKDOWN ROW
    private JPanel createPriceRow(String label, String value, boolean isDiscount, boolean isTotal) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("SansSerif", isTotal ? Font.BOLD : Font.PLAIN, isTotal ? 16 : 12));
        lbl.setForeground(isTotal ? TEXT_WHITE : TEXT_MUTED);

        JLabel val = new JLabel(value);
        val.setFont(new Font("SansSerif", Font.BOLD, isTotal ? 18 : 13));
        
        if (isTotal) {
            val.setForeground(ACCENT_RED);
        } else if (isDiscount) {
            val.setForeground(new Color(134, 239, 172));
        } else {
            val.setForeground(TEXT_WHITE);
        }

        row.add(lbl, BorderLayout.WEST);
        row.add(val, BorderLayout.EAST);
        return row;
    }

    //HELPER PANEL BARCODE GRAPHICS
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
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            int width = getWidth();
            int height = getHeight();
            
            int[] pattern = {2, 1, 3, 1, 4, 2, 1, 3, 2, 1, 4, 1, 2, 3, 1, 2, 4, 1, 3, 2, 1, 4, 2, 1, 3};

            int totalWidth = 0;
            for (int i = 0; i < pattern.length; i++) {
                totalWidth += pattern[i] + 2;
            }
            totalWidth -= 2;
            int currentX = Math.max(0, (width - totalWidth) / 2);

            g2.setColor(TEXT_WHITE);
            for (int i = 0; i < pattern.length; i++) {
                if (i % 2 == 0) {
                    g2.fillRect(currentX, 0, pattern[i], height);
                }
                currentX += pattern[i] + 2;
            }
            g2.dispose();
        }
    }

    //HELPER PANEL: WRAP CONTAINER FOR SEAT CHIPS
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
        SwingUtilities.invokeLater(() -> {
            new CinemaUI().setVisible(true);
        });
    }
}