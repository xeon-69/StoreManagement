package com.pos.system.dao;

import com.pos.system.models.Shift;
import com.pos.system.models.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class ShiftDAOTest extends BaseDAOTest {

    private ShiftDAO shiftDAO;
    private UserDAO userDAO;
    private int testUserId;

    @BeforeEach
    public void setUp() throws SQLException {
        shiftDAO = new ShiftDAO(connection);
        userDAO = new UserDAO(connection);

        // Preload a user for shifts
        User user = new User(0, "shiftuser", "pass", "CASHIER", false);
        userDAO.createUser(user);
        testUserId = userDAO.getUserByUsername("shiftuser").getId();
    }

    @Test
    public void testCreateShift() throws SQLException {
        // Arrange
        Shift shift = new Shift(0, testUserId, LocalDateTime.now(), null, 100.0, null, null, "OPEN");

        // Act
        Shift createdShift = shiftDAO.create(shift);

        // Assert
        assertTrue(createdShift.getId() > 0);
        assertEquals("OPEN", createdShift.getStatus());
        assertEquals(100.0, createdShift.getOpeningCash(), 0.001);
    }

    @Test
    public void testUpdateShift() throws SQLException {
        // Arrange
        Shift shift = new Shift(0, testUserId, LocalDateTime.now(), null, 150.0, null, null, "OPEN");
        shift = shiftDAO.create(shift);

        // Act
        shift.setStatus("CLOSED");
        shift.setEndTime(LocalDateTime.now().plusHours(8));
        shift.setExpectedClosingCash(500.0);
        shift.setActualClosingCash(495.0);
        shiftDAO.update(shift);

        Shift updatedShift = shiftDAO.findById(shift.getId());

        // Assert
        assertNotNull(updatedShift);
        assertEquals("CLOSED", updatedShift.getStatus());
        assertEquals(500.0, updatedShift.getExpectedClosingCash(), 0.001);
        assertEquals(495.0, updatedShift.getActualClosingCash(), 0.001);
        assertNotNull(updatedShift.getEndTime());
    }

    @Test
    public void testFindOpenShiftByUser() throws SQLException {
        // Arrange
        Shift closedShift = new Shift(0, testUserId, LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(16), 100.0, 500.0, 500.0, "CLOSED");
        shiftDAO.create(closedShift);

        Shift openShift = new Shift(0, testUserId, LocalDateTime.now(), null, 200.0, null, null, "OPEN");
        shiftDAO.create(openShift);

        // Act
        Shift foundShift = shiftDAO.findOpenShiftByUser(testUserId);

        // Assert
        assertNotNull(foundShift);
        assertEquals("OPEN", foundShift.getStatus());
        assertEquals(200.0, foundShift.getOpeningCash(), 0.001);
    }

    @Test
    public void testFindOpenShiftWhenNoneExists() throws SQLException {
        // Act
        Shift foundShift = shiftDAO.findOpenShiftByUser(testUserId);

        // Assert
        assertNull(foundShift);
    }
}
