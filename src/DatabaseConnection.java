import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import javax.swing.JOptionPane;

/**
 * Handles the connection to the MySQL database.
 */
public class DatabaseConnection {
    // --- IMPORTANT ---
    // Replace with your database details
    private static final String URL = "jdbc:mysql://localhost:3306/parkmaster_pro";
    private static final String USER = "root";       // Your MySQL username
    private static final String PASSWORD = "Raghul@2006"; // Your MySQL password

    /**
     * Establishes and returns a connection to the database.
     * @return A Connection object or null if an error occurs.
     */
    public static Connection getConnection() {
        try {
            // Register the JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Open a connection
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException | SQLException e) {
            // Show an error message to the user
            JOptionPane.showMessageDialog(null, 
                "Database Connection Failed: " + e.getMessage(), 
                "Database Error", 
                JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return null;
        }
    }
}