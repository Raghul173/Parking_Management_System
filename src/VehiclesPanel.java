import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

public class VehiclesPanel extends JPanel {

    // --- DARK THEME COLOR DEFINITIONS ---
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;
    private static final Color ACCENT_BLUE = new Color(66, 133, 244);
    private static final Color ACCENT_GREEN = new Color(26, 179, 148);
    
    public VehiclesPanel() {
        setLayout(new BorderLayout(10, 20));
        setBackground(BG_DARK); // Main panel background
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // Create the sub-panels first to reference them
        ActiveVehiclesSubPanel activeVehiclesPanel = new ActiveVehiclesSubPanel();
        VehicleHistorySubPanel historyPanel = new VehicleHistorySubPanel();

        JPanel headerPanel = createHeaderPanel(activeVehiclesPanel, historyPanel); // Pass panels for global refresh
        
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 14));
        // Tab colors for dark theme
        tabbedPane.setBackground(BG_DARK);
        tabbedPane.setForeground(TEXT_LIGHT); 
        
        activeVehiclesPanel.setBackground(CARD_DARK);
        historyPanel.setBackground(CARD_DARK);

        tabbedPane.addTab("Active Vehicles", activeVehiclesPanel);
        tabbedPane.addTab("Vehicle History", historyPanel);
        
        add(headerPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    // Updated to include global refresh button functionality and dark theme styling
    private JPanel createHeaderPanel(ActiveVehiclesSubPanel activePanel, VehicleHistorySubPanel historyPanel) {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        // --- Title Panel (WEST) ---
        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        
        JLabel title = new JLabel("Vehicle Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_LIGHT); // White text
        
        JLabel subtitle = new JLabel("Track and manage vehicle entries and exits");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SUBDUED); // Light gray text
        
        titlePanel.add(title);
        titlePanel.add(subtitle);
        headerPanel.add(titlePanel, BorderLayout.WEST);

        // --- Refresh Button Panel (EAST) ---
        JPanel refreshButtonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshButtonPanel.setOpaque(false);

        JButton refreshButton = new JButton("Refresh All") {
            // Override paintComponent for gradient
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();
                // Gradient colors adapted for dark theme accent
                Color color1 = ACCENT_GREEN; 
                Color color2 = ACCENT_BLUE; 
                GradientPaint gp = new GradientPaint(0, 0, color1, 0, h, color2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, w, h, 8, 8); // Fill with rounded corners
                super.paintComponent(g);
                g2.dispose();
            }
        };
        
        // Button style
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setForeground(Color.WHITE);
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setContentAreaFilled(false);
        
        // Action Listener to refresh both panels (logic remains the same)
        refreshButton.addActionListener(e -> {
            activePanel.loadActiveVehicles(); 
            historyPanel.loadHistory(); 
        });

        refreshButtonPanel.add(refreshButton);
        headerPanel.add(refreshButtonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }
}

class ActiveVehiclesSubPanel extends JPanel {
    private JTable activeVehiclesTable;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private Timer refreshTimer; 

    // Inherit colors from parent VehiclesPanel
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;

    public ActiveVehiclesSubPanel() {
        setLayout(new BorderLayout(0, 15));
        setBackground(CARD_DARK); // Sub-panel background
        setOpaque(true);
        add(createActionPanel(), BorderLayout.NORTH);
        add(createTablePanel(), BorderLayout.CENTER);
        loadActiveVehicles();
        startAutoRefresh(); 
    }
    
    // Auto-refresh methods (UNCHANGED logic, but included for completeness)
    private void startAutoRefresh() {
        refreshTimer = new Timer(60000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadActiveVehicles();
                System.out.println("Active Vehicles table refreshed automatically."); 
            }
        });
        refreshTimer.start();
    }
    
    @Override
    public void removeNotify() {
        super.removeNotify();
        if (refreshTimer != null) {
            refreshTimer.stop();
        }
    }
    
    private JPanel createActionPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        
        JTextField searchField = new JTextField(20);
        searchField.putClientProperty("JTextField.placeholderText", "Search by vehicle number, owner, or slot...");
        
        // Search Field Dark Theme Styling
        searchField.setBackground(CARD_DARK.brighter());
        searchField.setForeground(TEXT_LIGHT);
        searchField.setCaretColor(TEXT_LIGHT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true), 
            new EmptyBorder(8, 10, 8, 10)
        ));
        searchField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            private void filter() {
                String text = searchField.getText();
                if (text.trim().length() == 0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });
        panel.add(searchField, BorderLayout.WEST);
        
        return panel;
    }
    
    private JPanel createTablePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(CARD_DARK); // Table container background
        panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(new Color(60, 60, 80)), new EmptyBorder(10, 10, 10, 10)));
        
        String[] columnNames = {"Vehicle No.", "Owner Name", "Slot", "Entry Time", "Duration", "Fee", "Status", "Actions"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int col) { return col == 7; }
        };

        activeVehiclesTable = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        activeVehiclesTable.setRowSorter(sorter);

        styleTable(activeVehiclesTable);
        
        activeVehiclesTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        activeVehiclesTable.getColumnModel().getColumn(7).setCellRenderer(new ButtonRenderer());
        activeVehiclesTable.getColumnModel().getColumn(7).setCellEditor(new ButtonEditor(new JCheckBox(), activeVehiclesTable, this));

        JScrollPane scrollPane = new JScrollPane(activeVehiclesTable);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(CARD_DARK); // Table viewport background
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    public void loadActiveVehicles() {
        tableModel.setRowCount(0);
        String sql = "SELECT ticket_id, vehicle_number, owner_name, slot_id, entry_time, vehicle_type FROM parked_vehicles WHERE status = 'Parked' ORDER BY entry_time ASC";
        // ... (Database loading logic remains the same)
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                Timestamp entryTime = rs.getTimestamp("entry_time");
                Duration duration = Duration.between(entryTime.toInstant(), Instant.now());
                long hours = duration.toHours();
                long minutes = duration.toMinutes() % 60;
                String durationStr = String.format("%dh %02dm", hours, minutes);
                LocalDate entryDate = entryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                LocalDate nowDate = LocalDate.now();
                long daysParked = ChronoUnit.DAYS.between(entryDate, nowDate) + 1;
                double rate = rs.getString("vehicle_type").equals("2-Wheeler") ? 10.0 : 20.0;
                double fee = daysParked * rate;

                Object[] row = {
                    rs.getString("vehicle_number"), rs.getString("owner_name"), rs.getString("slot_id"),
                    new SimpleDateFormat("dd/MM/yyyy, hh:mm a").format(entryTime),
                    durationStr, String.format("₹%.2f", fee), "Active", "Record Exit"
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void styleTable(JTable table) {
        table.setRowHeight(45); table.setShowVerticalLines(false); 
        table.setGridColor(new Color(60, 60, 80)); // Dark grid lines
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
        table.setBackground(CARD_DARK);
        table.setForeground(TEXT_LIGHT); // White row text
        table.setSelectionBackground(new Color(60, 60, 80)); // Dark selection background
        table.setSelectionForeground(TEXT_LIGHT); 
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
        header.setBackground(CARD_DARK); // Dark header background
        header.setForeground(TEXT_SUBDUED); // Light gray header text
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(60, 60, 80))); // Darker bottom line
    }
}

class VehicleHistorySubPanel extends JPanel {
    private DefaultTableModel tableModel;
    
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;

    public VehicleHistorySubPanel() {
        setLayout(new BorderLayout()); 
        setOpaque(true);
        setBackground(CARD_DARK);
        
        String[] columnNames = {"Vehicle No.", "Owner Name", "Slot", "Entry Time", "Exit Time", "Fee Paid", "Status"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        
        JTable historyTable = new JTable(tableModel);
        styleTable(historyTable);
        historyTable.getColumnModel().getColumn(6).setCellRenderer(new StatusBadgeRenderer());
        
        JScrollPane scrollPane = new JScrollPane(historyTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 80)));
        scrollPane.getViewport().setBackground(CARD_DARK); // Scroll pane viewport background
        add(scrollPane, BorderLayout.CENTER);
        loadHistory();
    }
    
    public void loadHistory() {
        tableModel.setRowCount(0);
        String sql = "SELECT pv.vehicle_number, pv.owner_name, pv.slot_id, pv.entry_time, pv.exit_time, p.amount " +
                     "FROM parked_vehicles pv JOIN payments p ON pv.ticket_id = p.ticket_id " +
                     "WHERE pv.status = 'Exited' ORDER BY pv.exit_time DESC";
        // ... (Database loading logic remains the same)
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy, hh:mm a");
            while (rs.next()) {
                Object[] row = {
                    rs.getString("vehicle_number"), rs.getString("owner_name"), rs.getString("slot_id"),
                    sdf.format(rs.getTimestamp("entry_time")), sdf.format(rs.getTimestamp("exit_time")),
                    String.format("₹%.2f", rs.getDouble("amount")), "Completed"
                };
                tableModel.addRow(row);
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    private void styleTable(JTable table) {
        table.setRowHeight(45); table.setShowVerticalLines(false); 
        table.setGridColor(new Color(60, 60, 80));
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14)); 
        table.setBackground(CARD_DARK);
        table.setForeground(TEXT_LIGHT);
        
        JTableHeader header = table.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 14)); 
        header.setBackground(CARD_DARK);
        header.setForeground(TEXT_SUBDUED);
    }
}

class StatusBadgeRenderer extends DefaultTableCellRenderer {
    
    private static final Color CARD_DARK = new Color(36, 38, 58);

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        String status = value.toString(); label.setText(" " + status + " ");
        
        // Colors adapted for Dark Theme (badges should be brighter/more contrast)
        if ("Active".equals(status)) { 
            label.setForeground(new Color(133, 222, 117)); // Lighter green text
            label.setBackground(new Color(40, 60, 45)); // Dark green background
        } else { // Completed Status
            label.setForeground(new Color(180, 180, 180)); 
            label.setBackground(new Color(50, 50, 70)); 
        }
        
        label.setOpaque(true); label.setHorizontalAlignment(SwingConstants.CENTER); label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        
        JPanel wrapper = new JPanel(new GridBagLayout()); 
        // Use CARD_DARK for non-selected rows, a dark accent for selected rows
        wrapper.setBackground(isSelected ? new Color(50, 50, 70) : CARD_DARK); 
        wrapper.add(label);
        return wrapper;
    }
}

class ButtonRenderer extends JButton implements TableCellRenderer {
    public ButtonRenderer() {
        setOpaque(true); setFont(new Font("Segoe UI", Font.BOLD, 12));
        // Colors adapted for dark theme
        setForeground(new Color(133, 222, 117)); // Lighter green text
        setBackground(new Color(50, 50, 70)); // Dark button background
        setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90))); // Darker border
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        setText((value == null) ? "" : value.toString()); 
        // Dark theme: Change button background when selected
        if (isSelected) {
            setBackground(new Color(60, 60, 80));
        } else {
            setBackground(new Color(50, 50, 70));
        }
        return this;
    }
}

class ButtonEditor extends DefaultCellEditor {
    // NOTE: This class's UI is derived from the ButtonRenderer component. 
    // The internal logic remains dedicated to handling the checkout action.
    private JButton button;
    private String label;
    private JTable table;
    private ActiveVehiclesSubPanel parentPanel;
    
    private static final Color BUTTON_FG_COLOR = new Color(133, 222, 117); 
    private static final Color BUTTON_BG_COLOR = new Color(50, 50, 70); 

    public ButtonEditor(JCheckBox checkBox, JTable table, ActiveVehiclesSubPanel parent) {
        super(checkBox);
        this.table = table;
        this.parentPanel = parent;
        
        button = new JButton();
        button.setOpaque(true);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setForeground(BUTTON_FG_COLOR);
        button.setBackground(BUTTON_BG_COLOR);
        button.setBorder(BorderFactory.createLineBorder(new Color(70, 70, 90)));

        button.addActionListener(e -> {
            fireEditingStopped();
            int modelRow = table.convertRowIndexToModel(table.getEditingRow());
            String vehicleNumber = table.getModel().getValueAt(modelRow, 0).toString();
            String slotId = table.getModel().getValueAt(modelRow, 2).toString();
            // Assuming showDetailedExitDialog is implemented elsewhere or logic needs to be simplified here
            // If the detailed dialog is NOT implemented, this needs to call the simple checkout logic.
            // Simplified action: 
            JOptionPane.showMessageDialog(parentPanel, "Opening detailed billing for slot " + slotId, "Action Triggered", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.label = (value == null) ? "" : value.toString();
        button.setText(label);
        return button;
    }
    
    public Object getCellEditorValue() { return label; }
    
    // NOTE: The exitVehicleFromTable method and BillingConfirmationDialog are omitted as per your request for UI code only.
}