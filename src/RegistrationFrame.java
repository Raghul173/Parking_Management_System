import javax.swing.*;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegistrationFrame extends JFrame {

    private final JTextField usernameField;
    private final JPasswordField passwordField;
    private final JPasswordField confirmPasswordField;
    private final Runnable refreshCallback;

    // Constructor for when no callback is needed (e.g., if used from other places in the future)
    public RegistrationFrame() {
        this(null);
    }

    // Main constructor that accepts a callback to refresh the staff panel
    public RegistrationFrame(Runnable callback) {
        this.refreshCallback = callback;

        setTitle("Add New Staff"); // Title changed
        setSize(400, 300); // Height reduced as role selection is removed
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        JLabel titleLabel = new JLabel("Add New Staff");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 24));
        titleLabel.setBounds(100, 20, 200, 30);
        add(titleLabel);

        JLabel userLabel = new JLabel("Username:");
        userLabel.setBounds(50, 80, 80, 25);
        add(userLabel);

        usernameField = new JTextField(20);
        usernameField.setBounds(180, 80, 165, 25);
        add(usernameField);

        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setBounds(50, 120, 80, 25);
        add(passwordLabel);

        passwordField = new JPasswordField(20);
        passwordField.setBounds(180, 120, 165, 25);
        add(passwordField);

        JLabel confirmPasswordLabel = new JLabel("Confirm Password:");
        confirmPasswordLabel.setBounds(50, 160, 120, 25);
        add(confirmPasswordLabel);
        
        confirmPasswordField = new JPasswordField(20);
        confirmPasswordField.setBounds(180, 160, 165, 25);
        add(confirmPasswordField);

        JButton registerButton = new JButton("Add Staff");
        registerButton.setBounds(150, 220, 100, 30); // Position updated
        add(registerButton);

        registerButton.addActionListener(e -> registerUser());
    }

    private void registerUser() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        String confirmPassword = new String(confirmPasswordField.getPassword());
        String role = "Staff"; // Role is now hardcoded to "Staff"

        // (Validation logic remains the same)
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields are required.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!password.equals(confirmPassword)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String usernameRegex = "^[a-zA-Z][a-zA-Z0-9_]{2,19}$";
        if (!username.matches(usernameRegex)) {
            JOptionPane.showMessageDialog(this, "Invalid Username Format:\n- Must be 3-20 characters long.\n- Must start with a letter.\n- Can only contain letters, numbers, and underscores (_).", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (password.length() < 8) {
            JOptionPane.showMessageDialog(this, "Password must be at least 8 characters long.", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        boolean hasUppercase = !password.equals(password.toLowerCase());
        boolean hasLowercase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*()].*");
        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
            StringBuilder errorMessage = new StringBuilder("Invalid Password Format:\n");
            if (!hasUppercase) errorMessage.append("- Must contain at least one uppercase letter.\n");
            if (!hasLowercase) errorMessage.append("- Must contain at least one lowercase letter.\n");
            if (!hasDigit) errorMessage.append("- Must contain at least one number.\n");
            if (!hasSpecial) errorMessage.append("- Must contain at least one special character (!@#$%^&*()).\n");
            JOptionPane.showMessageDialog(this, errorMessage.toString(), "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.setString(3, role); // Role is always "Staff"
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows > 0) {
                JOptionPane.showMessageDialog(this, "New staff member added successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
                if (refreshCallback != null) {
                    refreshCallback.run();
                }
                this.dispose();
            }

        } catch (SQLException ex) {
            if (ex.getErrorCode() == 1062) {
                JOptionPane.showMessageDialog(this, "Username already exists.", "Registration Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Database error: " + ex.getMessage(), "Registration Error", JOptionPane.ERROR_MESSAGE);
            }
            ex.printStackTrace();
        }
    }
}