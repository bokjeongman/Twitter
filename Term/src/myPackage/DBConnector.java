package myPackage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnector {
    private static final String url = "jdbc:mysql://127.0.0.1/twitter";
    private static final String user = "root";
    private static final String password = "rokmc33wp##"; // User's DB password

    // JDBC Driver load
    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Cannot load MySQL JDBC Driver");
            throw new RuntimeException("JDBC Driver not found", e);
        }
    }

    // method that returns connection object
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}
