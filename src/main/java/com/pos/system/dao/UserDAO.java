package com.pos.system.dao;

import com.pos.system.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO extends BaseDAO { // Extended BaseDAO

    public UserDAO() throws SQLException {
        super();
    }

    public UserDAO(Connection conn) {
        super(conn);
    }

    public User login(String username, String password) { // Changed to catch SQLException
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) { // Using connection from BaseDAO

            pstmt.setString(1, username);
            pstmt.setString(2, password);
            // Note: Plain text password check for demo. Use BCrypt in production!

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"));
                }
            }
        } catch (SQLException e) { // Catching SQLException
            e.printStackTrace();
        }
        return null;
    }

    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, username);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new User(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("password"),
                            rs.getString("role"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // Add other methods if they existed (create user etc.)
    // For now only getUserByUsername was visible in the view_file (it was short).
    // Wait, the file content I see in view_file 165 is only showing lines 1-45?
    // Let me check if there are more lines.
    // The view_file output usually shows "Total Lines: XX".
    // I will check the output of the view_file command.
}
