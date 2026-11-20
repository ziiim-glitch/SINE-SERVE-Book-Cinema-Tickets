import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class ThemedDialog {

    private static final Color BG_CARD = new Color(0x1E, 0x18, 0x38);      // #1E1838
    private static final Color BORDER_VIOLET = new Color(0x7C, 0x3A, 0xED); // #7C3AED
    private static final Color GRADIENT_PINK = new Color(0xEC, 0x48, 0x99); // #EC4899
    private static final Color TEXT_TITLE = Color.WHITE;
    private static final Color TEXT_MSG = new Color(0xD1, 0xD5, 0xDB);      // #D1D5DB
    private static final Color ICON_SUCCESS = new Color(0x10, 0xB9, 0x81);  // Green
    private static final Color ICON_ERROR = new Color(0xEF, 0x44, 0x44);    // Red
    private static final Color ICON_INFO = new Color(0xA8, 0x55, 0xF7);     // Violet

    private enum DialogType {
        SUCCESS, INFO, ERROR, CONFIRM
    }

    public static void showSuccess(Component parent, String title, String message) {
        showInternal(parent, title, message, DialogType.SUCCESS);
    }

    public static void showInfo(Component parent, String title, String message) {
        String lower = (title + " " + message).toLowerCase();
        DialogType type = (lower.contains("successful") || lower.contains("created") || lower.contains("welcome") || lower.contains("confirmed"))
                ? DialogType.SUCCESS
                : DialogType.INFO;
        showInternal(parent, title, message, type);
    }

    public static void showError(Component parent, String title, String message) {
        showInternal(parent, title, message, DialogType.ERROR);
    }

    public static boolean showConfirm(Component parent, String title, String message) {
        return showInternal(parent, title, message, DialogType.CONFIRM);
    }

    private static boolean showInternal(Component parent, String title, String message, DialogType type) {
        Window owner = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (owner == null && parent instanceof Window) {
            owner = (Window) parent;
        }

        final boolean[] confirmed = {false};
        JDialog dialog = new JDialog(owner, title, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.setBackground(new Color(0, 0, 0, 0));

        dialog.getRootPane().registerKeyboardAction(
                e -> {
                    confirmed[0] = false;
                    dialog.dispose();
                },
                KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                JComponent.WHEN_IN_FOCUSED_WINDOW
        );

        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                UITheme.enableAntiAliasing(g2);

                int w = getWidth();
                int h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Float(1, 1, w - 3, h - 3, 22, 22);

                g2.setColor(BG_CARD);
                g2.fill(shape);

                g2.setStroke(new BasicStroke(1.5f));
                g2.setColor(BORDER_VIOLET);
                g2.draw(shape);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(24, 28, 22, 28));

        JLabel iconLabel = new JLabel(new DialogStatusIcon(type, 46));
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel(title, SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_TITLE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        String safeHtml = message
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", "<br>");
        JLabel msgLabel = new JLabel(
                "<html><div style='width:270px; text-align:center;'>" + safeHtml + "</div></html>",
                SwingConstants.CENTER
        );
        msgLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msgLabel.setForeground(TEXT_MSG);
        msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        btnRow.setOpaque(false);
        btnRow.setAlignmentX(Component.CENTER_ALIGNMENT);

        if (type == DialogType.CONFIRM) {
            UITheme.GradientButton cancelBtn = UITheme.GradientButton.createGhost("Cancel", BORDER_VIOLET, 14);
            cancelBtn.setPreferredSize(new Dimension(115, 38));
            cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            cancelBtn.addActionListener(e -> {
                confirmed[0] = false;
                dialog.dispose();
            });
            btnRow.add(cancelBtn);
        }

        UITheme.GradientButton okBtn = new UITheme.GradientButton("OK", BORDER_VIOLET, GRADIENT_PINK, 14);
        okBtn.setPreferredSize(new Dimension(type == DialogType.CONFIRM ? 115 : 140, 38));
        okBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        okBtn.addActionListener(e -> {
            confirmed[0] = true;
            dialog.dispose();
        });
        btnRow.add(okBtn);

        dialog.getRootPane().setDefaultButton(okBtn);

        card.add(iconLabel);
        card.add(Box.createRigidArea(new Dimension(0, 12)));
        card.add(titleLabel);
        card.add(Box.createRigidArea(new Dimension(0, 8)));
        card.add(msgLabel);
        card.add(Box.createRigidArea(new Dimension(0, 20)));
        card.add(btnRow);

        dialog.setContentPane(card);
        dialog.pack();
        dialog.setLocationRelativeTo(owner != null ? owner : parent);
        dialog.setVisible(true);

        return confirmed[0];
    }

    private static class DialogStatusIcon implements Icon {
        private final DialogType type;
        private final int size;

        public DialogStatusIcon(DialogType type, int size) {
            this.type = type;
            this.size = size;
        }

        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            UITheme.enableAntiAliasing(g2);
            g2.translate(x, y);

            Color accent = switch (type) {
                case SUCCESS -> ICON_SUCCESS;
                case ERROR -> ICON_ERROR;
                case INFO, CONFIRM -> ICON_INFO;
            };

            g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 42));
            g2.fill(new Ellipse2D.Double(2, 2, size - 4, size - 4));

            g2.setStroke(new BasicStroke(2f));
            g2.setColor(accent);
            g2.draw(new Ellipse2D.Double(2, 2, size - 4, size - 4));

            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int cx = size / 2;
            int cy = size / 2;

            switch (type) {
                case SUCCESS -> {
                    Path2D check = new Path2D.Float();
                    check.moveTo(cx - 8, cy);
                    check.lineTo(cx - 2, cy + 6);
                    check.lineTo(cx + 9, cy - 6);
                    g2.draw(check);
                }
                case ERROR -> {
                    g2.drawLine(cx - 6, cy - 6, cx + 6, cy + 6);
                    g2.drawLine(cx + 6, cy - 6, cx - 6, cy + 6);
                }
                case INFO, CONFIRM -> {
                    g2.fillOval(cx - 2, cy - 9, 4, 4);
                    g2.drawLine(cx, cy - 2, cx, cy + 8);
                }
            }

            g2.dispose();
        }
    }
}