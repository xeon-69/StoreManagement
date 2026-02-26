package com.pos.system.dao;

import com.pos.system.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserDAOTest extends BaseDAOTest {

    private UserDAO userDAO;

    @BeforeEach
    public void setUp() {
        // Initialize UserDAO with the BaseDAOTest database connection
        userDAO = new UserDAO(connection);

        // Ensure standard testing state. Note: The schema.sql inserts a default 'admin'
        // user.
    }

    @Test
    public void testCreateAndGetUser() {
        // Arrange
        User newUser = new User(0, "testuser", "testpass", "CASHIER", false);

        // Act
        boolean created = userDAO.createUser(newUser);
        User retrievedUser = userDAO.getUserByUsername("testuser");

        // Assert
        assertTrue(created, "User should be created successfully");
        assertNotNull(retrievedUser, "User should be retrievable by username");
        assertEquals("testuser", retrievedUser.getUsername());
        assertEquals("testpass", retrievedUser.getPassword());
        assertEquals("CASHIER", retrievedUser.getRole());
        assertFalse(retrievedUser.isForcePasswordChange());
    }

    @Test
    public void testLoginSuccess() {
        // Arrange
        User newUser = new User(0, "loginuser", "loginpass", "ADMIN", false);
        userDAO.createUser(newUser);

        // Act
        User loggedInUser = userDAO.login("loginuser", "loginpass");

        // Assert
        assertNotNull(loggedInUser, "Login should succeed with correct credentials");
        assertEquals("loginuser", loggedInUser.getUsername());
        assertTrue(loggedInUser.isAdmin(), "User should be ADMIN");
    }

    @Test
    public void testLoginFailure() {
        // Arrange
        User newUser = new User(0, "failuser", "failpass", "CASHIER", false);
        userDAO.createUser(newUser);

        // Act
        User loggedInUser = userDAO.login("failuser", "wrongpass");

        // Assert
        assertNull(loggedInUser, "Login should fail with incorrect password");
    }

    @Test
    public void testGetAllUsers() {
        // Arrange
        // (admin user already exists from schema.sql script)
        userDAO.createUser(new User(0, "user1", "pass", "CASHIER", false));
        userDAO.createUser(new User(0, "user2", "pass", "ADMIN", true));

        // Act
        List<User> users = userDAO.getAllUsers();

        // Assert
        assertTrue(users.size() >= 3, "Should have at least 3 users including the default admin");
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user1")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user2")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("admin")));
    }

    @Test
    public void testUpdateUserRole() {
        // Arrange
        userDAO.createUser(new User(0, "roleuser", "pass", "CASHIER", false));
        User user = userDAO.getUserByUsername("roleuser");

        // Act
        boolean updated = userDAO.updateUserRole(user.getId(), "ADMIN");
        User updatedUser = userDAO.getUserByUsername("roleuser");

        // Assert
        assertTrue(updated, "Role should be updated successfully");
        assertEquals("ADMIN", updatedUser.getRole(), "Role should reflect the update");
    }

    @Test
    public void testUpdateUserPassword() {
        // Arrange
        userDAO.createUser(new User(0, "passuser", "oldpass", "CASHIER", false));
        User user = userDAO.getUserByUsername("passuser");

        // Act
        boolean updated = userDAO.updateUserPassword(user.getId(), "newpasshash", true);
        User updatedUser = userDAO.getUserByUsername("passuser");

        // Assert
        assertTrue(updated, "Password should be updated successfully");
        assertEquals("newpasshash", updatedUser.getPassword(), "Password should reflect the update");
        assertTrue(updatedUser.isForcePasswordChange(), "Force password change flag should be set to true");
    }

    @Test
    public void testDeleteUser() {
        // Arrange
        userDAO.createUser(new User(0, "deluser", "pass", "CASHIER", false));
        User user = userDAO.getUserByUsername("deluser");

        // Act
        boolean deleted = userDAO.deleteUser(user.getId());
        User retrievedUser = userDAO.getUserByUsername("deluser");

        // Assert
        assertTrue(deleted, "User should be deleted successfully");
        assertNull(retrievedUser, "Deleted user should not be retrievable");
    }
}
