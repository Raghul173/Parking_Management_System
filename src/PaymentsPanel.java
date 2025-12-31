import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
// java.sql.DriverManager is required implicitly for DatabaseConnection access if static

public class PaymentsPanel extends JPanel {

    // --- DARK THEME COLOR DEFINITIONS ---
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;
    private static final Color ACCENT_BLUE = new Color(66, 133, 244);
    private static final Color ACCENT_GREEN = new Color(26, 179, 148);

    private JTable paymentsTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    
    private JLabel totalRevenueLabel;
    private JLabel twoWheelerCountLabel;
    private JLabel fourWheelerCountLabel;
    private JTextField searchField;

    public PaymentsPanel() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        setBackground(BG_DARK); // Main panel background

        add(createStatsPanel(), BorderLayout.NORTH);

        JPanel mainContentPanel = new JPanel(new BorderLayout(0, 15));
        mainContentPanel.setOpaque(false);
        
        mainContentPanel.add(createTablePanel(), BorderLayout.CENTER);
        mainContentPanel.add(createActionPanel(), BorderLayout.NORTH);

        add(mainContentPanel, BorderLayout.CENTER);

        loadPaymentData();
    }

    private JPanel createStatsPanel() {
        JPanel statsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        statsPanel.setOpaque(false);
        statsPanel.setPreferredSize(new Dimension(0, 100));

        totalRevenueLabel = new JLabel("₹0.00");
        totalRevenueLabel.setForeground(ACCENT_GREEN);
        statsPanel.add(createStatCard("Total Revenue", totalRevenueLabel, "$", CARD_DARK, ACCENT_GREEN));

        twoWheelerCountLabel = new JLabel("0");
        Color accentOrange = new Color(255, 165, 0); 
        twoWheelerCountLabel.setForeground(accentOrange);
        statsPanel.add(createStatCard("2-Wheelers Paid", twoWheelerCountLabel, "\uD83D\uDEF5", CARD_DARK, accentOrange));

        fourWheelerCountLabel = new JLabel("0");
        fourWheelerCountLabel.setForeground(ACCENT_BLUE);
        statsPanel.add(createStatCard("4-Wheelers Paid", fourWheelerCountLabel, "\uD83D\uDE97", CARD_DARK, ACCENT_BLUE));

        return statsPanel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, String iconText, Color bgColor, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(15, 0));
        card.setBackground(bgColor);
        
        // Dark theme border
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true),
            new EmptyBorder(15, 20, 15, 20)
        ));

        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        
        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLbl.setForeground(TEXT_SUBDUED); // Subdued text color
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        
        textPanel.add(titleLbl);
        textPanel.add(valueLabel);

        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
        iconLabel.setForeground(accentColor.brighter()); // Brighter icon accent
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);

        card.add(textPanel, BorderLayout.CENTER);
        card.add(iconLabel, BorderLayout.EAST);

        return card;
    }

    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);

        searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search by vehicle number...");
        
        // Search field dark theme styling
        searchField.setBackground(CARD_DARK);
        searchField.setForeground(TEXT_LIGHT);
        searchField.setCaretColor(TEXT_LIGHT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                // Filter based on the Vehicle Number column (index 2)
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 2)); 
                }
            }
        });

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);

        JButton refreshButton = new JButton("Refresh");
        // Dark theme refresh button color (subdued gray/blue)
        styleButton(refreshButton, new Color(90, 90, 110)); 
        refreshButton.addActionListener(e -> loadPaymentData());

        JButton downloadButton = new JButton("Download PDF");
        // Dark theme download button color (Accent Green)
        styleButton(downloadButton, ACCENT_GREEN); 
        downloadButton.addActionListener(e -> downloadAsPdf());

        btnPanel.add(refreshButton);
        btnPanel.add(downloadButton);

        panel.add(searchField, BorderLayout.WEST);
        panel.add(btnPanel, BorderLayout.EAST);
        
        return panel;
    }

    private void styleButton(JButton btn, Color color) {
        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setBackground(color);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(140, 35));
    }

    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_DARK); // Table panel background
        panel.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 80), 1)); // Dark border

        String[] columnNames = {
            "Slot ID", "Owner Name", "Vehicle Number", "Vehicle Type",
            "Entry Time", "Exit Time", "Amount Paid", "Status"
        };
        
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        paymentsTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        paymentsTable.setRowSorter(sorter);

        paymentsTable.setRowHeight(45);
        paymentsTable.setShowVerticalLines(false);
        paymentsTable.setGridColor(new Color(60, 60, 80)); // Dark grid lines
        paymentsTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        paymentsTable.setBackground(CARD_DARK); // Table rows background
        paymentsTable.setForeground(TEXT_LIGHT); // Table row text color
        paymentsTable.setSelectionBackground(new Color(60, 60, 80)); // Dark selection background
        paymentsTable.setSelectionForeground(TEXT_LIGHT);

        JTableHeader header = paymentsTable.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14));
        header.setBackground(CARD_DARK); // Header background
        header.setForeground(TEXT_SUBDUED); // Header text color
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 60, 80))); // Dark header line

        paymentsTable.getColumnModel().getColumn(7).setCellRenderer(new StatusBadgeRenderer());

        JScrollPane scrollPane = new JScrollPane(paymentsTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_DARK); // Scroll pane viewport background

        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private void loadPaymentData() {
        tableModel.setRowCount(0);
        
        // Query 1: Fetch Occupied (Pending) Vehicles
        String pendingSql = "SELECT slot_id, owner_name, vehicle_number, vehicle_type, entry_time " +
                            "FROM parked_vehicles WHERE status = 'Parked' ORDER BY entry_time DESC";

        // Query 2: Fetch Exited (Completed) Vehicles
        String historySql = "SELECT pv.slot_id, pv.owner_name, pv.vehicle_number, pv.vehicle_type, pv.entry_time, pv.exit_time, p.amount " +
                            "FROM payments p JOIN parked_vehicles pv ON p.ticket_id = pv.ticket_id WHERE pv.status = 'Exited' ORDER BY p.payment_time DESC";

        String summarySql = "SELECT SUM(p.amount) AS total_revenue, " +
                            "COUNT(CASE WHEN pv.vehicle_type = '2-Wheeler' THEN 1 END) AS two_wheeler_count, " +
                            "COUNT(CASE WHEN pv.vehicle_type = '4-Wheeler' THEN 1 END) AS four_wheeler_count " +
                            "FROM payments p JOIN parked_vehicles pv ON p.ticket_id = pv.ticket_id";

        // NOTE: DatabaseConnection class is assumed to exist and accessible.
        try (Connection conn = DatabaseConnection.getConnection()) {
            
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

            // 1. Load Pending Vehicles
            try (PreparedStatement pstmt = conn.prepareStatement(pendingSql); ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String entry = sdf.format(rs.getTimestamp("entry_time"));
                    // For pending vehicles: Exit time is "-", Amount is 0, Status is Pending
                    Object[] row = { 
                        rs.getString("slot_id"), 
                        rs.getString("owner_name"), 
                        rs.getString("vehicle_number"), 
                        rs.getString("vehicle_type"), 
                        entry, 
                        "-", 
                        "₹0.00", 
                        "Pending" 
                    };
                    tableModel.addRow(row);
                }
            }

            // 2. Load Completed Payments
            try (PreparedStatement pstmt = conn.prepareStatement(historySql); ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String entry = sdf.format(rs.getTimestamp("entry_time"));
                    String exit = sdf.format(rs.getTimestamp("exit_time"));
                    double amt = rs.getDouble("amount");
                    Object[] row = { rs.getString("slot_id"), rs.getString("owner_name"), rs.getString("vehicle_number"), rs.getString("vehicle_type"), entry, exit, String.format("₹%.2f", amt), "Completed"};
                    tableModel.addRow(row);
                }
            }

            try (PreparedStatement pstmt = conn.prepareStatement(summarySql); ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    totalRevenueLabel.setText(String.format("₹%.2f", rs.getDouble("total_revenue")));
                    twoWheelerCountLabel.setText(String.valueOf(rs.getInt("two_wheeler_count")));
                    fourWheelerCountLabel.setText(String.valueOf(rs.getInt("four_wheeler_count")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Database Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void downloadAsPdf() {
        if (paymentsTable.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "No data to export.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            MessageFormat header = new MessageFormat("Payment History Report");
            MessageFormat footer = new MessageFormat("Page {0,number,integer}");
            paymentsTable.print(JTable.PrintMode.FIT_WIDTH, header, footer);
        } catch (PrinterException pe) {
            JOptionPane.showMessageDialog(this, "Printing Error: " + pe.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    class StatusBadgeRenderer extends DefaultTableCellRenderer {
        
        private static final Color CARD_DARK = new Color(36, 38, 58);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            label.setText(" " + value.toString() + " ");
            
            String status = value.toString();

            if ("Pending".equals(status)) {
                // Status: Pending (Orange/Yellow for visibility)
                label.setForeground(new Color(255, 170, 50)); 
                label.setBackground(new Color(70, 50, 30));
            } else {
                // Status: Completed (Using subdued dark theme colors)
                label.setForeground(new Color(180, 180, 180)); 
                label.setBackground(new Color(50, 50, 70));
            }
            
            label.setOpaque(true);
            label.setHorizontalAlignment(SwingConstants.CENTER);
            label.setFont(new Font("Segoe UI", Font.BOLD, 12));
            
            JPanel wrapper = new JPanel(new GridBagLayout());
            // Dark selection color for the wrapper panel
            wrapper.setBackground(isSelected ? new Color(60, 60, 80) : CARD_DARK); 
            wrapper.add(label);
            return wrapper;
        }
    }
}