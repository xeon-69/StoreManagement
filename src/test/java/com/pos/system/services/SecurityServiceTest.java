package com.pos.system.services;

import com.pos.system.dao.AuditLogDAO;
import com.pos.system.models.AuditLog;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private AuditLogDAO auditLogDAOMock;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(auditLogDAOMock);
    }

    @Test
    void testHashPassword() {
        // Arrange
        String plainText = "mySecurePassword";

        // Act
        String hashedPassword = securityService.hashPassword(plainText);

        // Assert
        assertTrue(hashedPassword.startsWith("$2a$"));
        assertNotEquals(plainText, hashedPassword);
    }

    @Test
    void testVerifyPasswordSuccess() {
        // Arrange
        String plainText = "mypassword";
        String hashed = BCrypt.hashpw(plainText, BCrypt.gensalt(10));

        // Act
        boolean result = securityService.verifyPassword(plainText, hashed);

        // Assert
        assertTrue(result);
    }

    @Test
    void testVerifyPasswordFailure() {
        // Arrange
        String plainText = "wrongpassword";
        String hashed = BCrypt.hashpw("correctpassword", BCrypt.gensalt(10));

        // Act
        boolean result = securityService.verifyPassword(plainText, hashed);

        // Assert
        assertFalse(result);
    }

    @Test
    void testVerifyLegacyPlaintextPassword() {
        // Arrange
        String plainText = "legacypass";

        // Act
        boolean result = securityService.verifyPassword("legacypass", "legacypass");
        boolean resultFail = securityService.verifyPassword("wrongpass", "legacypass");

        // Assert
        assertTrue(result);
        assertFalse(resultFail);
    }

    @Test
    void testLogAction() throws SQLException {
        // Arrange
        int userId = 1;
        String action = "CREATE";
        String entityName = "User";
        String entityId = "2";
        String details = "Created new user";

        // Act
        securityService.logAction(userId, action, entityName, entityId, details);

        // Assert
        ArgumentCaptor<AuditLog> logCaptor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogDAOMock, times(1)).create(logCaptor.capture());

        AuditLog capturedLog = logCaptor.getValue();
        assertEquals(userId, capturedLog.getUserId());
        assertEquals(action, capturedLog.getAction());
        assertEquals(entityName, capturedLog.getEntityName());
        assertEquals(entityId, capturedLog.getEntityId());
        assertEquals(details, capturedLog.getDetails());
        assertNotNull(capturedLog.getCreatedAt());
    }
}
