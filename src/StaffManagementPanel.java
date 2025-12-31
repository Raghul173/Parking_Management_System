import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class StaffManagementPanel extends JPanel {

    // FIX: Added serialVersionUID to resolve serialization warning/error
    private static final long serialVersionUID = 1L; 

    // --- DARK THEME COLOR DEFINITIONS ---
    private static final Color BG_DARK = new Color(28, 30, 48);
    private static final Color CARD_DARK = new Color(36, 38, 58);
    private static final Color TEXT_LIGHT = Color.WHITE;
    private static final Color TEXT_SUBDUED = Color.LIGHT_GRAY;
    private static final Color ACCENT_BLUE = new Color(66, 133, 244);
    private static final Color ACCENT_RED = new Color(220, 20, 60);

    private final String loggedInAdmin;
    private final JPanel cardContainerPanel;

    public StaffManagementPanel(String loggedInAdminUsername) {
        this.loggedInAdmin = loggedInAdminUsername;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(15, 20, 20, 20));
        setBackground(BG_DARK); // Main panel background

        // --- TOP PANEL: TITLE AND "ADD USER" BUTTON ---
        JPanel topPanel = new JPanel(new BorderLayout(10, 10));
        topPanel.setOpaque(false);
        
        JLabel titleLabel = new JLabel("Staff Management");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        titleLabel.setForeground(TEXT_LIGHT); // White title
        
        JButton addUserButton = new JButton("Add New User");
        addUserButton.setFont(new Font("Segoe UI", Font.BOLD, 14));
        addUserButton.setForeground(TEXT_LIGHT);
        addUserButton.setBackground(ACCENT_BLUE); // Blue accent button
        addUserButton.setFocusPainted(false);
        addUserButton.setBorderPainted(false);
        addUserButton.setPreferredSize(new Dimension(180, 40));
        addUserButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        topPanel.add(titleLabel, BorderLayout.WEST);
        topPanel.add(addUserButton, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // --- CARD CONTAINER WITH SCROLL PANE ---
        // NOTE: WrapLayout is now correctly used, assuming it is defined in WrapLayout.java
        cardContainerPanel = new JPanel(new WrapLayout(WrapLayout.LEFT, 20, 20));
        cardContainerPanel.setBackground(BG_DARK); // Container background
        
        JScrollPane scrollPane = new JScrollPane(cardContainerPanel);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(60, 60, 80))); // Dark border
        scrollPane.getViewport().setBackground(BG_DARK); // Scroll viewport background
        add(scrollPane, BorderLayout.CENTER);

        // --- ACTION LISTENERS (FIXED: Direct integration) ---
        addUserButton.addActionListener(e -> {
            promptForNewUser();
        });
        
        loadUsers();
    }

    // --- NEW: FUNCTIONALITY TO PROMPT AND ADD USER ---
    private void promptForNewUser() {
        JTextField usernameField = new JTextField(15);
        JPasswordField passwordField = new JPasswordField(15);
        String[] roles = {"Staff", "Admin"};
        JComboBox<String> roleDropdown = new JComboBox<>(roles);
        
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 8));
        panel.add(new JLabel("Username:"));
        panel.add(usernameField);
        panel.add(new JLabel("Password:"));
        panel.add(passwordField);
        panel.add(new JLabel("Role:"));
        panel.add(roleDropdown);
        
        int result = JOptionPane.showConfirmDialog(this, panel, 
                "Register New Staff Account", JOptionPane.OK_CANCEL_OPTION, 
                JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword()).trim();
            String role = (String) roleDropdown.getSelectedItem();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Username and Password cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Execute insertion
            addNewUserToDatabase(username, password, role);
        }
    }
    
    private void addNewUserToDatabase(String username, String password, String role) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // NOTE: Replace with proper hashing (e.g., BCrypt) in a production application.
            
            pstmt.setString(1, username);
            pstmt.setString(2, password); 
            pstmt.setString(3, role);
            
            pstmt.executeUpdate();
            
            JOptionPane.showMessageDialog(this, "User '" + username + "' added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadUsers(); // Refresh the card panel
            
        } catch (SQLException e) {
            if (e.getErrorCode() == 1062) { // MySQL Duplicate entry error code
                JOptionPane.showMessageDialog(this, "Username '" + username + "' already exists.", "Database Error", JOptionPane.ERROR_MESSAGE);
            } else {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to add user: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    // --- END NEW FUNCTIONALITY ---

    // --- MAIN METHOD TO LOAD AND DISPLAY USER CARDS ---
    public void loadUsers() {
        cardContainerPanel.removeAll();
        
        String sql = "SELECT id, username, role FROM users ORDER BY id";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String username = rs.getString("username");
                String role = rs.getString("role");
                cardContainerPanel.add(createStaffCard(id, username, role));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Failed to load user data.", "Database Error", JOptionPane.ERROR_MESSAGE);
        }
        
        cardContainerPanel.revalidate();
        cardContainerPanel.repaint();
    }

    // --- METHOD TO CREATE A SINGLE STYLED STAFF CARD ---
    private JPanel createStaffCard(int id, String username, String role) {
        JPanel card = new JPanel(new BorderLayout(15, 15));
        card.setPreferredSize(new Dimension(300, 120));
        card.setBackground(CARD_DARK); // Dark card background
        
        // Dark theme border
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(60, 60, 80), 1, true),
            new EmptyBorder(10, 10, 10, 10)
        ));

        // Left side: ID/Icon area
        JLabel idLabel = new JLabel(String.format("%02d", id));
        idLabel.setFont(new Font("Segoe UI", Font.BOLD, 48));
        idLabel.setForeground(new Color(60, 60, 80)); // Very dark gray ID
        idLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(idLabel, BorderLayout.WEST);

        // Center: Info Panel
        JPanel infoPanel = new JPanel();
        infoPanel.setOpaque(false);
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.add(Box.createVerticalGlue());
        
        JLabel usernameLabel = new JLabel(username);
        usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        usernameLabel.setForeground(TEXT_LIGHT); // White username
        
        JLabel roleLabel = new JLabel(role);
        roleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        roleLabel.setForeground(TEXT_SUBDUED); // Light gray role
        
        infoPanel.add(usernameLabel);
        infoPanel.add(roleLabel);
        infoPanel.add(Box.createVerticalGlue());
        card.add(infoPanel, BorderLayout.CENTER);

        // Right side: Delete Button
        JButton deleteButton = new JButton("Delete");
        deleteButton.setFont(new Font("Segoe UI", Font.BOLD, 12));
        deleteButton.setBackground(ACCENT_RED); // Red accent button
        deleteButton.setForeground(TEXT_LIGHT);
        deleteButton.setFocusPainted(false);
        deleteButton.setBorderPainted(false);
        deleteButton.setPreferredSize(new Dimension(80, 30));
        deleteButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        deleteButton.addActionListener(e -> deleteUser(username));
        
        JPanel buttonWrapper = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        buttonWrapper.setOpaque(false);
        buttonWrapper.add(deleteButton);
        card.add(buttonWrapper, BorderLayout.EAST);

        return card;
    }

    private void deleteUser(String username) {
        if (username.equalsIgnoreCase(loggedInAdmin)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete the user '" + username + "'?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM users WHERE username = ?";
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {
                
                pstmt.setString(1, username);
                int affectedRows = pstmt.executeUpdate();

                if (affectedRows > 0) {
                    JOptionPane.showMessageDialog(this, "User '" + username + "' deleted successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
                    loadUsers();
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Failed to delete user.", "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}

// NOTE: The conflicting WrapLayout definition has been removed from this final block.
// Assuming WrapLayout.java is now a separate, accessible file in your project.