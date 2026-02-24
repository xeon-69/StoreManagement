package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.SettingsDAO;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class SettingsControllerTest {

    private SettingsDAO mockSettingsDAO;
    private Map<String, String> dummySettings;
    private SettingsController controller;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        // Create a regular mock instead of construction mock
        mockSettingsDAO = mock(SettingsDAO.class);
        dummySettings = new HashMap<>();
        dummySettings.put("store_name", "Test Store");
        dummySettings.put("currency_symbol", "USD");
        dummySettings.put("tax_rate", "5.0");
        dummySettings.put("printer_id", "COM2");

        doReturn(dummySettings).when(mockSettingsDAO).getAllSettings();
        doReturn(true).when(mockSettingsDAO).updateSetting(anyString(), anyString());

        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/settings.fxml"));

        // Use ControllerFactory to inject a subclass of the controller that returns the
        // mock DAO
        fxmlLoader.setControllerFactory(param -> {
            if (param == SettingsController.class) {
                return new SettingsController() {
                    @Override
                    protected SettingsDAO getSettingsDAO() throws SQLException {
                        return mockSettingsDAO;
                    }
                };
            }
            try {
                return param.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        // No construction mock to close
    }

    @Test
    public void testSettingsLoadAndSave(FxRobot robot) throws Exception {
        // Assert loaded values (wait up to 30s for background thread to populate
        // labels)
        System.err.println("DEBUG: Starting wait for settings load...");
        WaitForAsyncUtils.waitForFxEvents();
        try {
            WaitForAsyncUtils.waitFor(30, java.util.concurrent.TimeUnit.SECONDS, () -> {
                TextField field = robot.lookup("#storeNameField").queryAs(TextField.class);
                String text = field.getText();
                if (text != null && !text.isEmpty() && !"My Store".equals(text)) {
                    System.err.println("DEBUG: Field text changed to: " + text);
                }
                return "Test Store".equals(text);
            });
        } catch (Exception e) {
            String currentText = robot.lookup("#storeNameField").queryAs(TextField.class).getText();
            System.err.println("DEBUG: Settings timeout! Current text: '" + currentText + "'");
            throw e;
        }

        if (!"Test Store"
                .equals(robot.lookup("#storeNameField").queryAs(javafx.scene.control.TextField.class).getText())) {
            System.err.println("DEBUG: Settings never loaded. Current text: "
                    + robot.lookup("#storeNameField").queryAs(javafx.scene.control.TextField.class).getText());
        }

        FxAssert.verifyThat("#storeNameField", TextInputControlMatchers.hasText("Test Store"));
        FxAssert.verifyThat("#currencyField", TextInputControlMatchers.hasText("USD"));

        // Modify fields
        robot.doubleClickOn("#taxRateField").write("8.0");

        // Click save button (.btn-primary)
        robot.clickOn(".btn-primary");

        WaitForAsyncUtils.waitForFxEvents(); // wait for saveTask

        // Verify interactions on the injected mock
        verify(mockSettingsDAO, atLeastOnce()).getAllSettings();
        verify(mockSettingsDAO, times(1)).updateSetting("store_name", "Test Store");
        verify(mockSettingsDAO, times(1)).updateSetting("tax_rate", "8.0");
    }
}
