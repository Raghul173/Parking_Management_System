import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

public class ReportsPanel extends JPanel {

    // --- DARK THEME COLOR DEFINITIONS ---
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;
    private static final Color ACCENT_BLUE = new Color(66, 133, 244);
    private static final Color ACCENT_GREEN = new Color(26, 179, 148);

    // Labels for dynamic data
    private JLabel topOccupancyLabel, topRevenueLabel, topVehiclesLabel, topActiveLabel;
    private JLabel dailyCompletedLabel, dailyPendingLabel, dailyRevenueLabel;
    private JLabel occTotalLabel, occOccupiedLabel, occAvailableLabel, occRateLabel;
    private JLabel activityEntriesLabel, activityActiveLabel, activityExitsLabel, activityDurationLabel;

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 20));
        setBackground(BG_DARK); // Main panel background
        setBorder(new EmptyBorder(20, 20, 20, 20));

        add(createHeaderPanel(), BorderLayout.NORTH);
        add(createMainContentPanel(), BorderLayout.CENTER);

        // Load data on panel initialization
        loadReportData();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JPanel titlePanel = new JPanel(new GridLayout(2, 1));
        titlePanel.setOpaque(false);
        
        JLabel title = new JLabel("Reports & Analytics");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(TEXT_LIGHT); // White text
        
        JLabel subtitle = new JLabel("Generate and download detailed reports");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        subtitle.setForeground(TEXT_SUBDUED); // Light gray text
        
        titlePanel.add(title);
        titlePanel.add(subtitle);

        // Panel for Buttons (Export and Refresh)
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);

        // 1. Refresh Button
        JButton refreshButton = new GradientButton("Refresh");
        refreshButton.setPreferredSize(new Dimension(100, 40));
        refreshButton.addActionListener(e -> {
            loadReportData();
            JOptionPane.showMessageDialog(this, "Report data refreshed successfully.", "Refresh", JOptionPane.INFORMATION_MESSAGE);
        });
        buttonPanel.add(refreshButton);

        // 2. Export Button
        JButton exportButton = new GradientButton("Export All Reports");
        exportButton.setPreferredSize(new Dimension(180, 40));
        exportButton.addActionListener(e -> downloadAllReports());
        buttonPanel.add(exportButton);

        headerPanel.add(titlePanel, BorderLayout.WEST);
        headerPanel.add(buttonPanel, BorderLayout.EAST);
        
        return headerPanel;
    }

    private JPanel createMainContentPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(0, 20));
        mainPanel.setOpaque(false);

        JPanel topSummaryPanel = new JPanel(new GridLayout(1, 4, 20, 0));
        topSummaryPanel.setOpaque(false);
        topOccupancyLabel = new JLabel("0%");
        topRevenueLabel = new JLabel("₹0");
        topVehiclesLabel = new JLabel("0");
        topActiveLabel = new JLabel("0");
        
        topSummaryPanel.add(createTopSummaryCard("Occupancy", topOccupancyLabel, "\u25C6", true)); 
        topSummaryPanel.add(createTopSummaryCard("Revenue", topRevenueLabel, "₹", false)); 
        topSummaryPanel.add(createTopSummaryCard("Vehicles", topVehiclesLabel, "\u25C6", true)); 
        topSummaryPanel.add(createTopSummaryCard("Active", topActiveLabel, "\u25A1", false)); 
        
        mainPanel.add(topSummaryPanel, BorderLayout.NORTH);

        JPanel reportGridPanel = new JPanel(new GridLayout(2, 2, 20, 20));
        reportGridPanel.setOpaque(false);
        
        dailyCompletedLabel = new JLabel("0"); dailyPendingLabel = new JLabel("0"); dailyRevenueLabel = new JLabel("₹0");
        reportGridPanel.add(createReportCard("Daily Revenue Report", "Revenue", 
            createStatRow("Completed Payments", dailyCompletedLabel),
            createStatRow("Pending Payments", dailyPendingLabel),
            createHighlightedStatRow("Total Revenue", dailyRevenueLabel, new Color(45, 60, 45)) 
        ));
        
        occTotalLabel = new JLabel("0"); occOccupiedLabel = new JLabel("0"); occAvailableLabel = new JLabel("0"); occRateLabel = new JLabel("0%");
        reportGridPanel.add(createReportCard("Occupancy Report", "Occupancy", 
            createStatRow("Total Slots", occTotalLabel),
            createStatRow("Occupied Slots", occOccupiedLabel),
            createStatRow("Available Slots", occAvailableLabel),
            createHighlightedStatRow("Occupancy Rate", occRateLabel, new Color(45, 45, 60)) 
        ));

        activityEntriesLabel = new JLabel("0"); activityActiveLabel = new JLabel("0"); activityExitsLabel = new JLabel("0"); activityDurationLabel = new JLabel("0.0h");
        reportGridPanel.add(createReportCard("Vehicle Activity Report", "Activity", 
            createStatRow("Total Entries", activityEntriesLabel),
            createStatRow("Active Vehicles", activityActiveLabel),
            createStatRow("Completed Exits", activityExitsLabel),
            createHighlightedStatRow("Average Duration", activityDurationLabel, new Color(45, 60, 45)) 
        ));

        reportGridPanel.add(createPerformanceMetricsPanel());
        
        mainPanel.add(reportGridPanel, BorderLayout.CENTER);
        return mainPanel;
    }

    private JPanel createTopSummaryCard(String title, JLabel valueLabel, String iconText, boolean useBlueIcon) {
        JPanel card = new JPanel(new BorderLayout(10, 0));
        card.setBackground(CARD_DARK);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80)), 
            new EmptyBorder(15, 15, 15, 15)
        ));
        
        JPanel textPanel = new JPanel(new GridLayout(2, 1));
        textPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SUBDUED); 
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 28)); 
        valueLabel.setForeground(TEXT_LIGHT); 
        
        textPanel.add(titleLabel);
        textPanel.add(valueLabel); 

        JLabel iconLabel = new JLabel();
        String htmlIcon = "<html><font size='5'>" + iconText + "</font></html>";
        iconLabel.setText(htmlIcon);
        
        if (useBlueIcon) {
            iconLabel.setForeground(ACCENT_BLUE);
        } else {
            iconLabel.setForeground(TEXT_SUBDUED); 
        }
        iconLabel.setPreferredSize(new Dimension(30, 30));

        card.add(iconLabel, BorderLayout.WEST); 
        card.add(textPanel, BorderLayout.CENTER); 

        return card;
    }

    private JPanel createReportCard(String title, String reportType, Component... components) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(CARD_DARK); 
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80)), 
            new EmptyBorder(15, 15, 10, 15)
        ));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(TEXT_LIGHT); 
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createRigidArea(new Dimension(0, 15)));

        for (Component comp : components) {
            ((JComponent) comp).setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(comp);
            panel.add(Box.createRigidArea(new Dimension(0, 10)));
        }
        
        panel.add(Box.createVerticalGlue());
        
        JButton downloadButton = new JButton("Download " + reportType + " Report");
        styleDownloadButton(downloadButton, reportType);
        panel.add(downloadButton);
        return panel;
    }

    private JPanel createStatRow(String title, JLabel valueLabel) {
        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SUBDUED); 
        
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(TEXT_LIGHT); 
        
        row.add(titleLabel, BorderLayout.WEST);
        row.add(valueLabel, BorderLayout.EAST);
        return row;
    }
    
    private JPanel createHighlightedStatRow(String title, JLabel valueLabel, Color bgColor) {
        JPanel row = createStatRow(title, valueLabel);
        row.setBackground(bgColor);
        row.setOpaque(true);
        row.setBorder(new EmptyBorder(10, 10, 10, 10));
        ((JLabel)row.getComponent(0)).setForeground(TEXT_LIGHT); 
        ((JLabel)row.getComponent(1)).setFont(new Font("Segoe UI", Font.BOLD, 16));
        ((JLabel)row.getComponent(1)).setForeground(TEXT_LIGHT); 
        return row;
    }

    private JPanel createPerformanceMetricsPanel() {
        JPanel panel = createReportCard("Performance Metrics", "Performance",
            createProgressBarRow("System Uptime", 99, "99.8%"),
            createProgressBarRow("Processing Speed", 90, "Fast"),
            createProgressBarRow("User Satisfaction", 96, "4.8/5")
        );
        ((JButton) panel.getComponent(panel.getComponentCount() - 1)).setEnabled(false);
        return panel;
    }

    private JPanel createProgressBarRow(String title, int value, String text) {
        JPanel panel = new JPanel(new BorderLayout(0, 5)); 
        panel.setOpaque(false);
        
        JLabel titleLabel = new JLabel(title); 
        titleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        titleLabel.setForeground(TEXT_SUBDUED); 
        
        JLabel valueLabel = new JLabel(text); 
        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        valueLabel.setForeground(TEXT_LIGHT); 
        
        JPanel topRow = new JPanel(new BorderLayout()); 
        topRow.setOpaque(false);
        topRow.add(titleLabel, BorderLayout.WEST); 
        topRow.add(valueLabel, BorderLayout.EAST);
        
        JProgressBar progressBar = new JProgressBar(0, 100); 
        progressBar.setValue(value); 
        progressBar.setStringPainted(false);
        progressBar.setBackground(new Color(60, 60, 80)); 
        progressBar.setForeground(ACCENT_GREEN); 
        
        panel.add(topRow, BorderLayout.NORTH); 
        panel.add(progressBar, BorderLayout.SOUTH);
        return panel;
    }
    
    private void styleDownloadButton(JButton button, String reportType) {
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createLineBorder(new Color(90, 90, 110)));
        button.setBackground(CARD_DARK.brighter()); 
        button.setForeground(TEXT_LIGHT);
        button.setFocusPainted(false);
        button.addActionListener(e -> downloadReportAsPdf(reportType));
    }

    private void loadReportData() {
        try {
            String sql = "SELECT " +
                "(SELECT COUNT(*) FROM parking_slots) as total_slots, " +
                "(SELECT COUNT(*) FROM parked_vehicles WHERE status = 'Parked') as active_vehicles, " +
                "(SELECT COUNT(*) FROM payments WHERE DATE(payment_time) = CURDATE()) as completed_payments, " +
                "(SELECT COALESCE(SUM(amount), 0) FROM payments WHERE DATE(payment_time) = CURDATE()) as daily_revenue, " +
                "(SELECT COUNT(*) FROM parked_vehicles WHERE DATE(entry_time) = CURDATE()) as daily_entries, " +
                "(SELECT COUNT(*) FROM parked_vehicles WHERE status = 'Exited' AND DATE(exit_time) = CURDATE()) as daily_exits, " +
                "(SELECT COALESCE(AVG(TIMESTAMPDIFF(MINUTE, entry_time, exit_time)), 0) FROM parked_vehicles WHERE status = 'Exited' AND exit_time IS NOT NULL) as avg_mins";
            
            Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            
            if (rs.next()) {
                int totalSlots = rs.getInt("total_slots");
                int activeVehicles = rs.getInt("active_vehicles");
                int completedPayments = rs.getInt("completed_payments");
                double dailyRevenue = rs.getDouble("daily_revenue");
                int dailyEntries = rs.getInt("daily_entries");
                int dailyExits = rs.getInt("daily_exits");
                double avgMins = rs.getDouble("avg_mins");

                int occupancy = (totalSlots > 0) ? (activeVehicles * 100 / totalSlots) : 0;
                topOccupancyLabel.setText(occupancy + "%");
                topRevenueLabel.setText(String.format("₹%.0f", dailyRevenue));
                topVehiclesLabel.setText(String.valueOf(dailyEntries));
                topActiveLabel.setText(String.valueOf(activeVehicles));

                dailyCompletedLabel.setText(String.valueOf(completedPayments));
                dailyPendingLabel.setText(String.valueOf(activeVehicles));
                dailyRevenueLabel.setText(String.format("₹%.0f", dailyRevenue));
                
                occTotalLabel.setText(String.valueOf(totalSlots));
                occOccupiedLabel.setText(String.valueOf(activeVehicles));
                occAvailableLabel.setText(String.valueOf(totalSlots - activeVehicles));
                occRateLabel.setText(occupancy + "%");
                
                activityEntriesLabel.setText(String.valueOf(dailyEntries));
                activityActiveLabel.setText(String.valueOf(activeVehicles));
                activityExitsLabel.setText(String.valueOf(dailyExits));
                activityDurationLabel.setText(String.format("%.1fh", avgMins / 60.0));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }
    
    // --- MODIFIED METHOD: Explicitly sets size for off-screen table ---
    private void downloadReportAsPdf(String reportType) {
        JTable tableToPrint = new JTable(); 
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("Metric"); model.addColumn("Value");
        String reportTitle = "ParkMaster Pro - " + reportType + " Report";
        
        switch (reportType) {
            case "Revenue":
                model.addRow(new Object[]{"Completed Payments (Today)", dailyCompletedLabel.getText()});
                model.addRow(new Object[]{"Pending Payments (Active)", dailyPendingLabel.getText()});
                model.addRow(new Object[]{"Total Revenue (Today)", dailyRevenueLabel.getText()}); break;
            case "Occupancy":
                model.addRow(new Object[]{"Total Slots", occTotalLabel.getText()});
                model.addRow(new Object[]{"Occupied Slots", occOccupiedLabel.getText()});
                model.addRow(new Object[]{"Available Slots", occAvailableLabel.getText()});
                model.addRow(new Object[]{"Occupancy Rate", occRateLabel.getText()}); break;
            case "Activity":
                model.addRow(new Object[]{"Total Entries (Today)", activityEntriesLabel.getText()});
                model.addRow(new Object[]{"Active Vehicles (Now)", activityActiveLabel.getText()});
                model.addRow(new Object[]{"Completed Exits (Today)", activityExitsLabel.getText()});
                model.addRow(new Object[]{"Average Duration", activityDurationLabel.getText()}); break;
            default: JOptionPane.showMessageDialog(this, "This report cannot be downloaded.", "Info", JOptionPane.INFORMATION_MESSAGE); return;
        }
        tableToPrint.setModel(model);
        
        // FIX: Set size for the invisible table so the printer knows dimensions
        tableToPrint.setSize(500, tableToPrint.getRowHeight() * (model.getRowCount() + 1)); 

        try { 
            boolean complete = tableToPrint.print(JTable.PrintMode.FIT_WIDTH, new MessageFormat(reportTitle), new MessageFormat("Page {0,number,integer}")); 
            if (complete) {
                 JOptionPane.showMessageDialog(this, reportType + " Report Exported Successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } 
        catch (PrinterException pe) { 
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + pe.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE); 
        }
    }

    // --- MODIFIED METHOD: Explicitly sets size for off-screen table ---
    private void downloadAllReports() {
        DefaultTableModel allReportsModel = new DefaultTableModel();
        allReportsModel.addColumn("Metric");
        allReportsModel.addColumn("Value");

        // Add Revenue Data
        allReportsModel.addRow(new Object[]{"--- DAILY REVENUE REPORT ---", ""});
        allReportsModel.addRow(new Object[]{"Completed Payments (Today)", dailyCompletedLabel.getText()});
        allReportsModel.addRow(new Object[]{"Pending Payments (Active)", dailyPendingLabel.getText()});
        allReportsModel.addRow(new Object[]{"Total Revenue (Today)", dailyRevenueLabel.getText()});
        allReportsModel.addRow(new Object[]{"", ""}); 

        // Add Occupancy Data
        allReportsModel.addRow(new Object[]{"--- OCCUPANCY REPORT ---", ""});
        allReportsModel.addRow(new Object[]{"Total Slots", occTotalLabel.getText()});
        allReportsModel.addRow(new Object[]{"Occupied Slots", occOccupiedLabel.getText()});
        allReportsModel.addRow(new Object[]{"Available Slots", occAvailableLabel.getText()});
        allReportsModel.addRow(new Object[]{"Occupancy Rate", occRateLabel.getText()});
        allReportsModel.addRow(new Object[]{"", ""}); 

        // Add Activity Data
        allReportsModel.addRow(new Object[]{"--- VEHICLE ACTIVITY REPORT ---", ""});
        allReportsModel.addRow(new Object[]{"Total Entries (Today)", activityEntriesLabel.getText()});
        allReportsModel.addRow(new Object[]{"Active Vehicles (Now)", activityActiveLabel.getText()});
        allReportsModel.addRow(new Object[]{"Completed Exits (Today)", activityExitsLabel.getText()});
        allReportsModel.addRow(new Object[]{"Average Duration", activityDurationLabel.getText()});

        JTable tableToPrint = new JTable(allReportsModel);
        
        // FIX: Set specific dimensions. Since this table isn't added to a ScrollPane or Frame,
        // it has 0 width/height by default, causing print to fail.
        tableToPrint.setSize(500, tableToPrint.getRowHeight() * (allReportsModel.getRowCount() + 5));
        tableToPrint.getColumnModel().getColumn(0).setPreferredWidth(300);
        tableToPrint.getColumnModel().getColumn(1).setPreferredWidth(200);

        try {
            MessageFormat header = new MessageFormat("ParkMaster Pro - Full Analytics Summary");
            MessageFormat footer = new MessageFormat("Page {0,number,integer}");
            
            // This opens the standard Print Dialog. User selects "Microsoft Print to PDF"
            boolean complete = tableToPrint.print(JTable.PrintMode.FIT_WIDTH, header, footer);
            
            if (complete) {
                JOptionPane.showMessageDialog(this, "All Reports Exported Successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (PrinterException pe) {
            JOptionPane.showMessageDialog(this, "Error generating PDF: " + pe.getMessage(), "PDF Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}