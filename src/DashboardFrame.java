import javax.swing.*;
import java.awt.*;

public class DashboardFrame extends JFrame {

    public DashboardFrame(String username, String role) {

        setTitle("ParkMaster Pro - Dashboard");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- Header Panel ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(24, 119, 242));
        headerPanel.setPreferredSize(new Dimension(getWidth(), 60));

        JLabel titleLabel = new JLabel("  ParkMaster Pro");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        headerPanel.add(titleLabel, BorderLayout.WEST);

        JPanel userPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        userPanel.setOpaque(false);
        JLabel loggedInLabel = new JLabel("Logged in as: " + username + " (" + role + ")");
        loggedInLabel.setForeground(Color.WHITE);
        loggedInLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        JButton logoutButton = new JButton("Logout");
        logoutButton.setBackground(new Color(214, 69, 65));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.addActionListener(e -> {
            this.dispose();
            new LoginFrame().setVisible(true);
        });

        userPanel.add(loggedInLabel);
        userPanel.add(logoutButton);
        headerPanel.add(userPanel, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // --- Tabbed Pane for main content ---
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Arial", Font.BOLD, 14));

        // --- Create and Add Panels ---
        tabbedPane.addTab("Dashboard", new DashboardPanel());
        tabbedPane.addTab("Parking Slots", new ParkingSlotsPanel(role));
        tabbedPane.addTab("Vehicles", new VehiclesPanel());
        tabbedPane.addTab("Payments", new PaymentsPanel());
        
        // --- NEW: Reports & Analytics Tab ---
        tabbedPane.addTab("Reports", new ReportsPanel());
        
        // Staff Management Panel (Admin Only)
        if ("Admin".equalsIgnoreCase(role)) {
            tabbedPane.addTab("Staff Management", new StaffManagementPanel(username));
        }
        
        add(tabbedPane, BorderLayout.CENTER);

        // --- Footer Panel ---
        JPanel footerPanel = new JPanel();
        footerPanel.setBackground(Color.LIGHT_GRAY);
        JLabel footerLabel = new JLabel("© 2025 ParkMaster Pro - Parking Management System");
        footerPanel.add(footerLabel);
        add(footerPanel, BorderLayout.SOUTH);
    }
}