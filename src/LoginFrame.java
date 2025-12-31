import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class LoginFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JRadioButton adminRadioButton;
    private final JRadioButton staffRadioButton;

    public LoginFrame() {
        setTitle("ParkMaster Pro - Login");
        setSize(500, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel headerPanel = new JPanel();
        headerPanel.setBackground(new Color(24, 119, 242));
        headerPanel.setPreferredSize(new Dimension(500, 100));
        JLabel headerLabel = new JLabel("ParkMaster Pro");
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(new Font("Arial", Font.BOLD, 36));
        headerPanel.add(headerLabel);
        add(headerPanel, BorderLayout.NORTH);

        JPanel loginPanel = new JPanel();
        loginPanel.setLayout(null);
        add(loginPanel, BorderLayout.CENTER);

        JLabel loginLabel = new JLabel("Login");
        loginLabel.setFont(new Font("Arial", Font.BOLD, 28));
        loginLabel.setBounds(210, 20, 100, 35);
        loginPanel.add(loginLabel);

        // --- Role Selection Radio Buttons ---
        adminRadioButton = new JRadioButton("Login as Admin");
        adminRadioButton.setFont(new Font("Arial", Font.BOLD, 14));
        adminRadioButton.setBounds(100, 80, 150, 25);
        
        staffRadioButton = new JRadioButton("Login as Staff");
        staffRadioButton.setFont(new Font("Arial", Font.BOLD, 14));
        staffRadioButton.setBounds(260, 80, 150, 25);
        staffRadioButton.setSelected(true); // Staff is the default selection

        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(adminRadioButton);
        roleGroup.add(staffRadioButton);
        
        loginPanel.add(adminRadioButton);
        loginPanel.add(staffRadioButton);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        userLabel.setBounds(100, 130, 80, 25);
        loginPanel.add(userLabel);

        usernameField = new JTextField(20);
        usernameField.setBounds(100, 155, 300, 30);
        loginPanel.add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        passwordLabel.setBounds(100, 200, 80, 25);
        loginPanel.add(passwordLabel);

        passwordField = new JPasswordField(20);
        passwordField.setBounds(100, 225, 300, 30);
        loginPanel.add(passwordField);

        JButton loginButton = new JButton("Login");
        loginButton.setBounds(175, 290, 150, 40);
        loginButton.setBackground(new Color(24, 119, 242));
        loginButton.setForeground(Color.WHITE);
        loginButton.setFont(new Font("Arial", Font.BOLD, 16));
        loginPanel.add(loginButton);
        
        // --- Action Listeners ---
        loginButton.addActionListener(e -> authenticateUser());

        // Listener to auto-fill username for Admin
        adminRadioButton.addActionListener(e -> {
            usernameField.setText("Raghul");
            usernameField.setEditable(false);
        });

        staffRadioButton.addActionListener(e -> {
            usernameField.setText("");
            usernameField.setEditable(true);
            usernameField.requestFocus(); // Focus on username field for staff
        });
    }

    private void authenticateUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String selectedRole = adminRadioButton.isSelected() ? "Admin" : "Staff";

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username and Password cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Modified SQL to check for role as well, making it more secure
        String sql = "SELECT role FROM users WHERE username = ? AND password = ? AND role = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, selectedRole); // Add role to the query

            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String role = rs.getString("role");
                SwingUtilities.invokeLater(() -> {
                    new DashboardFrame(username, role).setVisible(true);
                    this.dispose();
                });
            } else {
                JOptionPane.showMessageDialog(this, "Invalid credentials for the selected role.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            }

        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Database query failed: " + ex.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}