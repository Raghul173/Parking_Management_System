import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.print.*; // Added for PDF/Printing functionality
import java.sql.*;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class ParkingSlotsPanel extends JPanel {
    private final String userRole;
    private final JPanel slotsContainerPanel;
    private final JTextField searchField;

    // --- DARK THEME COLOR DEFINITIONS ---
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;

    // Slot Colors adapted for dark theme
    private final Color COLOR_AVAILABLE_BG = new Color(45, 60, 75); // Dark Blue-Gray for card BG
    private final Color COLOR_AVAILABLE_BORDER = new Color(26, 179, 148); // Green Accent
    private final Color COLOR_OCCUPIED_BG = new Color(45, 60, 75); 
    private final Color COLOR_OCCUPIED_BORDER = new Color(220, 20, 60); // Red Accent
    private final Color COLOR_HOVER_BORDER = new Color(66, 133, 244); // Blue Accent

    public ParkingSlotsPanel(String role) {
        this.userRole = role;
        setLayout(new BorderLayout(10, 20));
        setBackground(BG_DARK); // Main panel background
        setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));

        searchField = new JTextField("Search by slot ID, type, or status...", 25);
        addPlaceholderStyle(searchField);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { refreshSlots(); }
            public void removeUpdate(DocumentEvent e) { refreshSlots(); }
            public void changedUpdate(DocumentEvent e) { refreshSlots(); }
        });
        
        // Style the search field for dark theme
        searchField.setBackground(CARD_DARK);
        searchField.setForeground(TEXT_LIGHT);
        searchField.setCaretColor(TEXT_LIGHT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80)),
            BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        add(createHeaderPanel(), BorderLayout.NORTH);

        slotsContainerPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, 20, 20));
        slotsContainerPanel.setBackground(BG_DARK); // Slot container background
        JScrollPane scrollPane = new JScrollPane(slotsContainerPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getViewport().setBackground(BG_DARK); // Scroll pane viewport background
        add(scrollPane, BorderLayout.CENTER);

        refreshSlots();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout(20, 10));
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));
        titlePanel.setOpaque(false);
        
        // Title and Subtitle styling for dark theme
        JLabel titleLabel = new JLabel("Parking Slots");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_LIGHT); 
        
        JLabel subtitleLabel = new JLabel("Manage and monitor all parking slots");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitleLabel.setForeground(TEXT_SUBDUED);
        
        titlePanel.add(titleLabel);
        titlePanel.add(subtitleLabel);

        JPanel searchAddPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        searchAddPanel.setOpaque(false);
        
        // --- ADD SLOT BUTTON ---
        JButton addSlotButton = new GradientButton("+ Add New Slot");
        addSlotButton.setPreferredSize(new Dimension(160, 40)); 
        addSlotButton.addActionListener(e -> promptForSlotTypeAndAdd());

        // --- DELETE SLOT BUTTON ---
        JButton deleteSlotButton = new GradientButton("- Delete Slot");
        deleteSlotButton.setPreferredSize(new Dimension(160, 40));
        deleteSlotButton.addActionListener(e -> promptForSlotDeletion());

        if (!"Admin".equalsIgnoreCase(userRole)) {
            addSlotButton.setVisible(false);
            deleteSlotButton.setVisible(false);
        }

        searchAddPanel.add(searchField);
        searchAddPanel.add(addSlotButton);
        searchAddPanel.add(deleteSlotButton); 

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(searchAddPanel, BorderLayout.EAST);
        return headerPanel;
    }

    // --- The rest of the file remains the same... ---
    
    private void refreshSlots() {
        slotsContainerPanel.removeAll();
        String searchText = searchField.getText().toLowerCase();
        if (searchText.equals("search by slot id, type, or status...")) {
            searchText = "";
        }

        Map<String, String> occupiedVehicleDetails = getOccupiedVehicleDetails();
        String sql = "SELECT slot_id, vehicle_type, is_occupied FROM parking_slots ORDER BY slot_id";

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String slotId = rs.getString("slot_id");
                String vehicleType = rs.getString("vehicle_type");
                boolean isOccupied = rs.getBoolean("is_occupied");
                String status = isOccupied ? "occupied" : "available";

                if (!searchText.isEmpty() && !(slotId.toLowerCase().contains(searchText) || vehicleType.toLowerCase().contains(searchText) || status.contains(searchText))) {
                    continue;
                }

                JPanel slotCard = createSlotCard(slotId, vehicleType, isOccupied);
                if (isOccupied) {
                    slotCard.setToolTipText(occupiedVehicleDetails.get(slotId));
                }
                addCardListeners(slotCard, slotId, vehicleType, isOccupied);
                slotsContainerPanel.add(slotCard);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading parking slots.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }

        slotsContainerPanel.revalidate();
        slotsContainerPanel.repaint();
    }

    private Map<String, String> getOccupiedVehicleDetails() {
        Map<String, String> details = new HashMap<>();
        String sql = "SELECT slot_id, owner_name, vehicle_number FROM parked_vehicles WHERE status = 'Parked'";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                String tooltipText = String.format("<html><b>Owner:</b> %s<br><b>Vehicle:</b> %s</html>", rs.getString("owner_name"), rs.getString("vehicle_number"));
                details.put(rs.getString("slot_id"), tooltipText);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return details;
    }

    private void addCardListeners(JPanel slotCard, String slotId, String vehicleType, boolean isOccupied) {
        if ("Admin".equalsIgnoreCase(userRole)) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem deleteItem = new JMenuItem("Delete Slot");
            deleteItem.addActionListener(e -> deleteSlot(slotId));
            popupMenu.add(deleteItem);
            slotCard.setComponentPopupMenu(popupMenu);
        }

        slotCard.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (isOccupied) {
                        exitVehicleDialog(slotId);
                    } else {
                        parkVehicleDialog(slotId, vehicleType);
                    }
                }
            }
            // Use dark theme colors for hover/exit
            @Override public void mouseEntered(MouseEvent e) { slotCard.setBorder(new RoundedBorder(COLOR_HOVER_BORDER, 15, 2)); }
            @Override public void mouseExited(MouseEvent e) { Color borderColor = isOccupied ? COLOR_OCCUPIED_BORDER : COLOR_AVAILABLE_BORDER; slotCard.setBorder(new RoundedBorder(borderColor, 15, 2));}
        });
    }
    
    private JPanel createSlotCard(String slotId, String vehicleType, boolean isOccupied) {
        JPanel card = new JPanel(new GridBagLayout()); card.setPreferredSize(new Dimension(220, 80)); card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Use dark theme colors
        Color bgColor = COLOR_AVAILABLE_BG; // Both occupied/available use the dark card background
        Color borderColor = isOccupied ? COLOR_OCCUPIED_BORDER : COLOR_AVAILABLE_BORDER;
        
        card.setBackground(bgColor); card.setBorder(new RoundedBorder(borderColor, 15, 2)); GridBagConstraints gbc = new GridBagConstraints();
        
        // Icon (uses dark theme colors/font)
        String iconText = "2-Wheeler".equals(vehicleType) ? "\uD83C\uDFCD" : "\uD83D\uDE97"; 
        JLabel iconLabel = new JLabel(iconText);
        iconLabel.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 30)); 
        iconLabel.setForeground(isOccupied ? TEXT_LIGHT : COLOR_AVAILABLE_BORDER); // Light color for icons
        
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridheight = 2; gbc.insets = new Insets(0, 10, 0, 10); card.add(iconLabel, gbc);
        
        // Slot ID Label
        JLabel slotIdLabel = new JLabel(slotId); 
        slotIdLabel.setFont(new Font("Segoe UI", Font.BOLD, 18)); 
        slotIdLabel.setForeground(TEXT_LIGHT); // White text
        
        gbc.gridx = 1; gbc.gridy = 0; gbc.gridheight = 1;
        gbc.anchor = GridBagConstraints.SOUTHWEST; gbc.insets = new Insets(0, 0, 0, 0); card.add(slotIdLabel, gbc);
        
        // Vehicle Type Label
        JLabel typeLabel = new JLabel(vehicleType); 
        typeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12)); 
        typeLabel.setForeground(TEXT_SUBDUED); // Subdued gray text
        
        gbc.gridx = 1; gbc.gridy = 1; gbc.anchor = GridBagConstraints.NORTHWEST; card.add(typeLabel, gbc);
        
        // Status Label (Pill style - colors adapted)
        String statusText = isOccupied ? "Occupied" : "Available"; 
        Color statusColor = isOccupied ? new Color(220, 20, 60) : new Color(26, 179, 148);
        
        JLabel statusLabel = new JLabel(statusText); 
        statusLabel.setForeground(Color.WHITE); 
        statusLabel.setBackground(statusColor); 
        statusLabel.setOpaque(true);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER); 
        statusLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2; gbc.anchor = GridBagConstraints.EAST; gbc.weightx = 1.0; gbc.insets = new Insets(0, 10, 0, 10); card.add(statusLabel, gbc);
        return card;
    }
    
    private void parkVehicleDialog(String slotId, String vehicleType) {
        JTextField ownerNameField = new JTextField(); JTextField vehicleNumberField = new JTextField(); 
        JPanel panel = new JPanel(new GridLayout(0, 1));
        
        // Ensure dialog content is legible on dark background systems if L&F uses system settings
        panel.setBackground(Color.WHITE); 
        
        panel.add(new JLabel("Vehicle Owner Name:")); panel.add(ownerNameField); panel.add(new JLabel("Vehicle Number (Format: TN-01-A-1234):")); panel.add(vehicleNumberField);
        while (true) {
            int result = JOptionPane.showConfirmDialog(this, panel, "Enter Vehicle Information for Slot " + slotId, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) return;
            String ownerName = ownerNameField.getText().trim(); String vehicleNumber = vehicleNumberField.getText().trim().toUpperCase();
            if (ownerName.isEmpty() || vehicleNumber.isEmpty()) { JOptionPane.showMessageDialog(this, "Both fields are required. Please try again.", "Input Error", JOptionPane.ERROR_MESSAGE); continue; }
            String vehicleNumberRegex = "^TN-\\d{2}-[A-Z]{1,2}-\\d{4}$";
            if (!vehicleNumber.matches(vehicleNumberRegex)) { JOptionPane.showMessageDialog(this, "Invalid Vehicle Number Format.\nPlease use the format: TN-01-A-1234 and try again.", "Input Error", JOptionPane.ERROR_MESSAGE); continue; }
            processVehicleParking(slotId, vehicleType, ownerName, vehicleNumber); break;
        }
    }

    private void processVehicleParking(String slotId, String vehicleType, String ownerName, String vehicleNumber) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection(); conn.setAutoCommit(false);
            String updateSlotSql = "UPDATE parking_slots SET is_occupied = true WHERE slot_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(updateSlotSql)) { pstmt.setString(1, slotId); pstmt.executeUpdate(); }
            String insertVehicleSql = "INSERT INTO parked_vehicles (slot_id, owner_name, vehicle_number, vehicle_type, entry_time, status) VALUES (?, ?, ?, ?, NOW(), 'Parked')";
            try (PreparedStatement pstmt = conn.prepareStatement(insertVehicleSql)) { pstmt.setString(1, slotId); pstmt.setString(2, ownerName); pstmt.setString(3, vehicleNumber); pstmt.setString(4, vehicleType); pstmt.executeUpdate(); }
            conn.commit(); JOptionPane.showMessageDialog(this, "Vehicle parked successfully in slot " + slotId, "Success", JOptionPane.INFORMATION_MESSAGE); refreshSlots();
        } catch (SQLException ex) {
            try { if (conn != null) conn.rollback(); } catch (SQLException se) { se.printStackTrace(); }
            ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Database error during vehicle parking.", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally { try { if (conn != null) conn.close(); } catch (SQLException se) { se.printStackTrace(); } }
    }
    
    // --- UPDATED METHOD: exitVehicleDialog ---
    private void exitVehicleDialog(String slotId) {
        String sql = "SELECT ticket_id, owner_name, vehicle_number, vehicle_type, entry_time FROM parked_vehicles WHERE slot_id = ? AND status = 'Parked'";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, slotId); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int ticketId = rs.getInt("ticket_id"); 
                String vehicleType = rs.getString("vehicle_type"); 
                Timestamp entryTime = rs.getTimestamp("entry_time");
                String ownerName = rs.getString("owner_name");
                String vehicleNumber = rs.getString("vehicle_number");
                
                LocalDate entryDate = entryTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); 
                LocalDate exitDate = LocalDate.now();
                long daysParked = ChronoUnit.DAYS.between(entryDate, exitDate) + 1; 
                double rate = "2-Wheeler".equalsIgnoreCase(vehicleType) ? 10.0 : 20.0;
                double totalAmount = daysParked * rate;
                
                String message = String.format("<html><b>Vehicle Details:</b><br><br>Slot: %s<br>Owner: %s<br>Vehicle No: %s<br>Entry Time: %s<br><br><b>Billing Details:</b><br><br>Parking Duration: %d Day(s)<br>Rate: %.2f / Day<br><b>Total Amount: %.2f</b><br><br>Confirm vehicle checkout and payment?</html>", 
                        slotId, ownerName, vehicleNumber, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(entryTime), daysParked, rate, totalAmount);
                
                // 1. Confirm Exit
                int confirmExit = JOptionPane.showConfirmDialog(this, message, "Confirm Vehicle Exit & Payment", JOptionPane.YES_NO_OPTION);
                
                if (confirmExit == JOptionPane.YES_OPTION) {
                    // 2. Ask to save PDF
                    int savePdf = JOptionPane.showConfirmDialog(this, "Do you want to save the bill as a PDF?", "Save Bill", JOptionPane.YES_NO_OPTION);
                    
                    if (savePdf == JOptionPane.YES_OPTION) {
                        printBill(ticketId, slotId, ownerName, vehicleNumber, entryTime, totalAmount);
                    }
                    
                    // 3. Checkout (remove from DB)
                    checkoutVehicle(slotId, ticketId, totalAmount); 
                }
            } else { JOptionPane.showMessageDialog(this, "Could not find active vehicle details for this slot.", "Error", JOptionPane.ERROR_MESSAGE); }
        } catch (SQLException ex) { ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Database error fetching vehicle details.", "Database Error", JOptionPane.ERROR_MESSAGE); }
    }

    // --- NEW METHOD: printBill (Handles PDF Generation via PrinterJob) ---
    private void printBill(int ticketId, String slotId, String owner, String vehicleNo, Timestamp entryTime, double amount) {
        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("Parking Bill - " + slotId);
        
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) return Printable.NO_SUCH_PAGE;
            
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            
            // Draw Bill Content
            g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
            int y = 50;
            g2d.drawString("PARKING RECEIPT", 100, y);
            
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
            y += 30; g2d.drawString("--------------------------------", 50, y);
            y += 20; g2d.drawString("Ticket ID : " + ticketId, 50, y);
            y += 20; g2d.drawString("Date      : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), 50, y);
            y += 20; g2d.drawString("--------------------------------", 50, y);
            y += 20; g2d.drawString("Slot ID   : " + slotId, 50, y);
            y += 20; g2d.drawString("Owner     : " + owner, 50, y);
            y += 20; g2d.drawString("Vehicle   : " + vehicleNo, 50, y);
            y += 20; g2d.drawString("Entry     : " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(entryTime), 50, y);
            y += 30; g2d.drawString("TOTAL AMOUNT : Rs. " + String.format("%.2f", amount), 50, y);
            y += 20; g2d.drawString("--------------------------------", 50, y);
            y += 30; g2d.drawString("Thank you for parking with us!", 60, y);

            return Printable.PAGE_EXISTS;
        });

        // Open System Print Dialog (User selects 'Microsoft Print to PDF' or 'Save as PDF')
        if (job.printDialog()) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this, "Bill sent to printer/saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this, "Error generating bill: " + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void checkoutVehicle(String slotId, int ticketId, double amount) {
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection(); conn.setAutoCommit(false);
            String sql1 = "UPDATE parking_slots SET is_occupied = false WHERE slot_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql1)) { pstmt.setString(1, slotId); pstmt.executeUpdate(); }
            String sql2 = "UPDATE parked_vehicles SET exit_time = NOW(), status = 'Exited' WHERE ticket_id = ? AND status = 'Parked'";
            try (PreparedStatement pstmt = conn.prepareStatement(sql2)) { pstmt.setInt(1, ticketId); pstmt.executeUpdate(); }
            String sql3 = "INSERT INTO payments (ticket_id, amount, payment_time) VALUES (?, ?, NOW())";
            try (PreparedStatement pstmt = conn.prepareStatement(sql3)) { pstmt.setInt(1, ticketId); pstmt.setDouble(2, amount); pstmt.executeUpdate(); }
            conn.commit(); JOptionPane.showMessageDialog(this, "Vehicle from slot " + slotId + " checked out successfully.\nAmount Paid: " + String.format("%.2f", amount), "Success", JOptionPane.INFORMATION_MESSAGE); refreshSlots();
        } catch (SQLException ex) {
            try { if (conn != null) conn.rollback(); } catch (SQLException se) { se.printStackTrace(); }
            ex.printStackTrace(); JOptionPane.showMessageDialog(this, "Database error during vehicle checkout.", "Database Error", JOptionPane.ERROR_MESSAGE);
        } finally { try { if (conn != null) conn.close(); } catch (SQLException se) { se.printStackTrace(); } }
    }

    private void promptForSlotTypeAndAdd() {
        String[] options = {"2-Wheeler", "4-Wheeler"};
        int choice = JOptionPane.showOptionDialog(this, "Select the type of slot to add:", "Select Vehicle Type", JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (choice != JOptionPane.CLOSED_OPTION) { addSlot(options[choice]); }
    }
    
    private void promptForSlotDeletion() {
        String slotId = JOptionPane.showInputDialog(this, "Enter Slot ID to delete (e.g., T-01):", "Delete Slot", JOptionPane.PLAIN_MESSAGE);
        if (slotId != null && !slotId.trim().isEmpty()) {
            deleteSlot(slotId.trim());
        }
    }

    private void addSlot(String vehicleType) {
        String prefix = vehicleType.equals("2-Wheeler") ? "T-" : "F-";
        int maxSlots = vehicleType.equals("2-Wheeler") ? 200 : 100;
        String slotNumStr = JOptionPane.showInputDialog(this, "Enter Slot Number (1-" + maxSlots + "):", "Add New " + vehicleType + " Slot", JOptionPane.PLAIN_MESSAGE);
        if (slotNumStr == null || slotNumStr.trim().isEmpty()) return;
        try {
            int slotNum = Integer.parseInt(slotNumStr);
            if (slotNum <= 0 || slotNum > maxSlots) { JOptionPane.showMessageDialog(this, "Slot number is exceeding the given limit (1-" + maxSlots + ").", "Input Error", JOptionPane.ERROR_MESSAGE); return; }
            String slotId = prefix + String.format("%02d", slotNum); String sql = "INSERT INTO parking_slots (slot_id, vehicle_type, is_occupied) VALUES (?, ?, ?)";
            try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, slotId); pstmt.setString(2, vehicleType); pstmt.setBoolean(3, false); pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Slot " + slotId + " added successfully.", "Success", JOptionPane.INFORMATION_MESSAGE); refreshSlots();
            } catch (SQLException ex) { if (ex.getErrorCode() == 1062) { JOptionPane.showMessageDialog(this, "Slot " + slotId + " already exists.", "Error", JOptionPane.ERROR_MESSAGE); } else { JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); } }
        } catch (NumberFormatException e) { JOptionPane.showMessageDialog(this, "Invalid number format.", "Input Error", JOptionPane.ERROR_MESSAGE); }
    }

    private void deleteSlot(String slotId) {
        int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete slot '" + slotId + "'? This cannot be undone.", "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;
        String sql = "DELETE FROM parking_slots WHERE slot_id = ? AND is_occupied = false";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, slotId);
            if (pstmt.executeUpdate() > 0) {
                JOptionPane.showMessageDialog(this, "Slot " + slotId + " deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE); refreshSlots();
            } else { checkDeletionFailureReason(slotId); }
        } catch (SQLException ex) { JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE); }
    }
    
    private void checkDeletionFailureReason(String slotId) throws SQLException {
        String sql = "SELECT is_occupied FROM parking_slots WHERE slot_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, slotId); ResultSet rs = pstmt.executeQuery();
            if (rs.next()) { JOptionPane.showMessageDialog(this, "Cannot delete an occupied slot (" + slotId + ").", "Deletion Failed", JOptionPane.ERROR_MESSAGE); } 
            else { JOptionPane.showMessageDialog(this, "Slot " + slotId + " not found.", "Deletion Failed", JOptionPane.ERROR_MESSAGE); }
        }
    }

    private void addPlaceholderStyle(JTextField textField) {
        Font placeholderFont = new Font(textField.getFont().getName(), Font.ITALIC, textField.getFont().getSize());
        textField.setForeground(TEXT_SUBDUED); // Dark theme placeholder color
        textField.setFont(placeholderFont);
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (textField.getForeground() == TEXT_SUBDUED) {
                    textField.setText("");
                    textField.setForeground(TEXT_LIGHT); // White text when typing
                    textField.setFont(textField.getFont().deriveFont(Font.PLAIN));
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (textField.getText().isEmpty()) {
                    textField.setForeground(TEXT_SUBDUED);
                    textField.setFont(placeholderFont);
                    textField.setText("Search by slot ID, type, or status...");
                }
            }
        });
    }
}

// Custom Gradient Button for modern look (kept as is, it uses dark colors)
class GradientButton extends JButton {
    public GradientButton(String text) {
        super(text);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setForeground(Color.WHITE);
        setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color color1 = new Color(24, 119, 242);
        Color color2 = new Color(46, 204, 113);
        GradientPaint gp = new GradientPaint(0, 0, color1, getWidth(), 0, color2);
        g2.setPaint(gp);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
        g2.dispose();
        super.paintComponent(g);
    }
}

// RoundedBorder class (unchanged)
class RoundedBorder implements Border {
    private int radius; private Color color; private int strokeWidth;
    public RoundedBorder(Color color, int radius, int strokeWidth) { this.radius = radius; this.color = color; this.strokeWidth = strokeWidth; }
    public Insets getBorderInsets(Component c) { return new Insets(this.radius+1, this.radius+1, this.radius+2, this.radius); }
    public boolean isBorderOpaque() { return true; }
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create(); g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(this.color); g2.setStroke(new BasicStroke(this.strokeWidth));
        g2.drawRoundRect(x + strokeWidth/2, y + strokeWidth/2, width-strokeWidth, height-strokeWidth, radius, radius); g2.dispose();
    }
}