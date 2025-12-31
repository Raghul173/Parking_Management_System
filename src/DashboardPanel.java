import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;

public class DashboardPanel extends JPanel {

    // Define Dark Theme Colors
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color ACCENT_BLUE = new Color(66, 133, 244); // For active stats
    private static final Color ACCENT_GREEN = new Color(26, 179, 148); // For available/revenue

    // Labels for the main statistics
    private JLabel totalSlotsLabel;
    private JLabel availableSlotsLabel;
    private JLabel occupiedSlotsLabel;
    private JLabel activeVehiclesLabel;
    private JLabel todaysRevenueLabel;
    private JLabel completedPaymentsLabel;
    private JLabel occupancyRateLabel;
    private JLabel occupancyDescLabel;
    private JProgressBar occupancyProgressBar;
    private JPanel recentEntriesPanel; // Panel to hold the list of recent entries

    public DashboardPanel() {
        setLayout(new BorderLayout(15, 15)); // Increased spacing
        setBackground(BG_DARK);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // --- Header Panel with Title AND Refresh Button ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Dashboard");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));
        titleLabel.setForeground(TEXT_LIGHT);
        
        // --- NEW: Refresh Button ---
        JButton refreshButton = new JButton("\u27F3 Refresh"); // Unicode symbol for refresh
        refreshButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        refreshButton.setForeground(TEXT_LIGHT);
        refreshButton.setBackground(new Color(60, 60, 80)); // Dark button background
        refreshButton.setFocusPainted(false);
        refreshButton.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        refreshButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        refreshButton.setPreferredSize(new Dimension(120, 40));

        // ********** MODIFICATION: REMOVED SUCCESS MESSAGE **********
        refreshButton.addActionListener(e -> {
            loadDashboardData();
            // Removed: JOptionPane.showMessageDialog(this, "Dashboard data refreshed.", "Refresh Status", JOptionPane.INFORMATION_MESSAGE);
        });
        
        headerPanel.add(titleLabel, BorderLayout.WEST);
        headerPanel.add(refreshButton, BorderLayout.EAST); // Added button to the header
        add(headerPanel, BorderLayout.NORTH);

        // --- Main content area with a GridBagLayout ---
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 5, 10, 5); // Reduced vertical padding
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;

        // --- Top Row Stats Panels (4 wide, using StatCard) ---
        gbc.gridy = 0;
        
        // 1. Total Slots (Icon: Car, Color: Default/Blue)
        gbc.gridx = 0;
        contentPanel.add(createStatCard("Total S.lots", "...", CARD_DARK, "\uD83D\uDE97", ACCENT_BLUE), gbc);
        totalSlotsLabel = ((StatCard) contentPanel.getComponent(0)).getValueLabel();

        // 2. Available Slots (Icon: None, Color: Green)
        gbc.gridx = 1;
        contentPanel.add(createStatCard("Available Slots", "...", CARD_DARK, "", ACCENT_GREEN), gbc);
        availableSlotsLabel = ((StatCard) contentPanel.getComponent(1)).getValueLabel();

        // 3. Occupied Slots (Icon: None, Color: White/Gray)
        gbc.gridx = 2;
        contentPanel.add(createStatCard("Occupied Slots", "...", CARD_DARK, "", TEXT_LIGHT), gbc);
        occupiedSlotsLabel = ((StatCard) contentPanel.getComponent(2)).getValueLabel();

        // 4. Active Vehicles (Icon: Refresh/Active, Color: Blue)
        gbc.gridx = 3;
        contentPanel.add(createStatCard("Active Vehicles", "...", CARD_DARK, "\u21C6", ACCENT_BLUE), gbc); // ↔ symbol for "Active"
        activeVehiclesLabel = ((StatCard) contentPanel.getComponent(3)).getValueLabel();


        // --- Second Row Panels (2 wide) ---
        gbc.gridy = 1;
        gbc.weighty = 1.5; // Give revenue and occupancy more height

        // 5. Today's Revenue Panel
        gbc.gridx = 0;
        gbc.gridwidth = 2;
        JPanel revenuePanel = createTitledPanel("Today's Revenue", CARD_DARK);
        
        todaysRevenueLabel = new JLabel("₹0.00", SwingConstants.LEFT);
        todaysRevenueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        todaysRevenueLabel.setForeground(ACCENT_GREEN); // Set to Green
        
        completedPaymentsLabel = new JLabel("From 0 completed payments", SwingConstants.LEFT);
        completedPaymentsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        completedPaymentsLabel.setForeground(Color.GRAY);
        
        revenuePanel.add(todaysRevenueLabel, BorderLayout.CENTER);
        revenuePanel.add(completedPaymentsLabel, BorderLayout.SOUTH);
        contentPanel.add(revenuePanel, gbc);

        // 6. Occupancy Rate Panel
        gbc.gridx = 2;
        gbc.gridwidth = 2;
        JPanel occupancyPanel = createTitledPanel("Occupancy Rate", CARD_DARK);
        
        occupancyRateLabel = new JLabel("0%", SwingConstants.LEFT);
        occupancyRateLabel.setFont(new Font("Segoe UI", Font.BOLD, 36));
        occupancyRateLabel.setForeground(TEXT_LIGHT); // White
        
        occupancyDescLabel = new JLabel("0 of 2 slots occupied", SwingConstants.LEFT);
        occupancyDescLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        occupancyDescLabel.setForeground(Color.GRAY);
        
        occupancyProgressBar = new JProgressBar(0, 100);
        occupancyProgressBar.setValue(0);
        occupancyProgressBar.setStringPainted(false);
        occupancyProgressBar.setForeground(ACCENT_BLUE); // Blue progress bar
        occupancyProgressBar.setBackground(BG_DARK); // Dark background for the bar track
        
        JPanel occupancyBottomPanel = new JPanel(new BorderLayout());
        occupancyBottomPanel.setOpaque(false);
        occupancyBottomPanel.add(occupancyDescLabel, BorderLayout.NORTH);
        occupancyBottomPanel.add(occupancyProgressBar, BorderLayout.SOUTH);
        
        occupancyPanel.add(occupancyRateLabel, BorderLayout.CENTER);
        occupancyPanel.add(occupancyBottomPanel, BorderLayout.SOUTH);
        contentPanel.add(occupancyPanel, gbc);

        // --- Recent Vehicle Entries Panel (Full Width) ---
        gbc.gridy = 2;
        gbc.gridx = 0;
        gbc.gridwidth = 4;
        gbc.weighty = 2.0; // Give it more space
        
        JPanel recentEntriesContainer = createTitledPanel("Recent Vehicle Entries", CARD_DARK);
        recentEntriesPanel = new JPanel();
        recentEntriesPanel.setLayout(new BoxLayout(recentEntriesPanel, BoxLayout.Y_AXIS));
        recentEntriesPanel.setOpaque(false);
        
        // To ensure the scroll pane background is also dark:
        JScrollPane scrollPane = new JScrollPane(recentEntriesPanel);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setBackground(CARD_DARK); // Dark background for the scrollable area
        scrollPane.getViewport().setOpaque(true);
        recentEntriesContainer.add(scrollPane, BorderLayout.CENTER);
        
        contentPanel.add(recentEntriesContainer, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // Load all data from the database
        loadDashboardData();
    }
    
    // Method to load all data in one go
    private void loadDashboardData() {
        // --- 1. Get Slot Statistics ---
        String slotSql = "SELECT COUNT(*) AS total_slots, COUNT(CASE WHEN is_occupied = TRUE THEN 1 END) AS occupied_slots FROM parking_slots";
        int totalSlots = 0, occupiedSlots = 0;
        // NOTE: DatabaseConnection is assumed to be defined elsewhere
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(slotSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                totalSlots = rs.getInt("total_slots");
                occupiedSlots = rs.getInt("occupied_slots");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        int availableSlots = totalSlots - occupiedSlots;
        totalSlotsLabel.setText(String.valueOf(totalSlots));
        availableSlotsLabel.setText(String.valueOf(availableSlots));
        occupiedSlotsLabel.setText(String.valueOf(occupiedSlots));
        activeVehiclesLabel.setText(String.valueOf(occupiedSlots)); // Active vehicles = occupied slots
        
        // Update Occupancy Rate
        if (totalSlots > 0) {
            int rate = (int) ((occupiedSlots * 100.0) / totalSlots);
            occupancyRateLabel.setText(rate + "%");
            occupancyProgressBar.setValue(rate);
            occupancyDescLabel.setText(occupiedSlots + " of " + totalSlots + " slots occupied");
        } else {
            occupancyRateLabel.setText("0%");
            occupancyProgressBar.setValue(0);
            occupancyDescLabel.setText("0 of 0 slots occupied"); // Changed to match the image text
        }

        // --- 2. Get Today's Revenue ---
        String revenueSql = "SELECT SUM(amount) AS todays_revenue, COUNT(*) AS completed_payments FROM payments WHERE DATE(payment_time) = CURDATE()";
        // NOTE: DatabaseConnection is assumed to be defined elsewhere
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(revenueSql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                double revenue = rs.getDouble("todays_revenue");
                int payments = rs.getInt("completed_payments");
                todaysRevenueLabel.setText(String.format("₹%.2f", revenue));
                completedPaymentsLabel.setText("From " + payments + " completed payments");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // --- 3. Get Recent Vehicle Entries ---
        recentEntriesPanel.removeAll();
        String recentEntriesSql = "SELECT owner_name, vehicle_number, slot_id, entry_time FROM parked_vehicles WHERE status = 'Parked' ORDER BY entry_time DESC LIMIT 4";
        // NOTE: DatabaseConnection and SimpleDateFormat are assumed to be defined elsewhere
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(recentEntriesSql);
             ResultSet rs = pstmt.executeQuery()) {
            SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a");
            while (rs.next()) {
                String vehicleNumber = rs.getString("vehicle_number");
                String ownerName = rs.getString("owner_name");
                String slotId = rs.getString("slot_id");
                String entryTime = sdf.format(rs.getTimestamp("entry_time"));
                recentEntriesPanel.add(createRecentEntryItem(vehicleNumber, ownerName, slotId, entryTime));
                recentEntriesPanel.add(Box.createRigidArea(new Dimension(0, 5))); // Spacer
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        recentEntriesPanel.revalidate();
        recentEntriesPanel.repaint();
    }

    // Helper method to create the top statistic cards
    private StatCard createStatCard(String title, String value, Color bgColor, String icon, Color valueColor) {
        return new StatCard(title, value, bgColor, icon, valueColor);
    }
    
    // Helper method to create a standardized panel with a title (Modified for dark theme)
    private JPanel createTitledPanel(String title, Color bgColor) {
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.setBackground(bgColor);
        panel.setBorder(new EmptyBorder(15, 15, 15, 15)); // Removed the line border

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        titleLabel.setForeground(Color.GRAY); // Subdued text color for title
        panel.add(titleLabel, BorderLayout.NORTH);
        return panel;
    }
    
    // Helper method to create a single item for the "Recent Entries" list
    private JPanel createRecentEntryItem(String vehicleNumber, String ownerName, String slot, String time) {
        JPanel itemPanel = new JPanel(new BorderLayout());
        itemPanel.setOpaque(false);
        itemPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);
        
        JLabel vehicleLabel = new JLabel(vehicleNumber);
        vehicleLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        vehicleLabel.setForeground(TEXT_LIGHT);
        
        JLabel ownerLabel = new JLabel(ownerName);
        ownerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ownerLabel.setForeground(Color.GRAY);
        
        leftPanel.add(vehicleLabel);
        leftPanel.add(ownerLabel);
        
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));
        rightPanel.setOpaque(false);
        
        JLabel slotLabel = new JLabel(slot);
        slotLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        slotLabel.setForeground(ACCENT_BLUE);
        
        JLabel timeLabel = new JLabel(time);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(Color.GRAY);
        
        rightPanel.add(slotLabel);
        rightPanel.add(timeLabel);
        
        itemPanel.add(leftPanel, BorderLayout.WEST);
        itemPanel.add(rightPanel, BorderLayout.EAST);
        
        return itemPanel;
    }
}

// A custom component class for the statistic cards to keep the code clean
class StatCard extends JPanel {
    private JLabel valueLabel;
    private JLabel iconLabel;

    public StatCard(String title, String value, Color bgColor, String iconString, Color valueColor) {
        // Use a simpler layout that gives space for the title/icon on top and value below
        setLayout(new BorderLayout(5, 5)); 
        setBackground(bgColor);
        // Removed borders to match the clean dark theme look
        setBorder(new EmptyBorder(15, 15, 15, 15)); 

        // --- Title Panel (Icon + Title) ---
        JPanel titleIconPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        titleIconPanel.setOpaque(false);
        
        // 1. Icon Setup
        iconLabel = new JLabel(iconString);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20)); // Larger icon font
        
        // Custom styling for icons to match the image: Icon is gray/white, except for "Active"
        if (iconString.equals("\u21C6")) { // Active symbol
            iconLabel.setForeground(new Color(66, 133, 244)); // Blue
        } else {
            iconLabel.setForeground(Color.GRAY); // Subdued icon color
        }
        
        // 2. Title Setup
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(Color.GRAY);
        
        titleIconPanel.add(iconLabel);
        titleIconPanel.add(titleLabel);
        
        // 3. Value Setup
        valueLabel = new JLabel(value);
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 36)); // Very large font
        valueLabel.setForeground(valueColor);

        // --- Add components to StatCard ---
        add(titleIconPanel, BorderLayout.NORTH);
        add(valueLabel, BorderLayout.WEST); // Place value label prominently on the left
    }
    
    public JLabel getValueLabel() {
        return valueLabel;
    }
}