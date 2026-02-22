package com.pos.system.controllers;

import com.pos.system.App;
import com.pos.system.dao.SettingsDAO;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedConstruction;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;
import org.testfx.matcher.control.TextInputControlMatchers;
import org.testfx.util.WaitForAsyncUtils;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(ApplicationExtension.class)
public class SettingsControllerTest {

    private MockedConstruction<SettingsDAO> mockedSettingsDAO;
    private SettingsController controller;

    @Start
    public void start(Stage stage) throws Exception {
        App.setLocale("en");

        mockedSettingsDAO = mockConstruction(SettingsDAO.class, (mock, context) -> {
            Map<String, String> dummySettings = new HashMap<>();
            dummySettings.put("store_name", "Test Store");
            dummySettings.put("currency_symbol", "USD");
            dummySettings.put("tax_rate", "5.0");
            dummySettings.put("printer_id", "COM2");
            when(mock.getAllSettings()).thenReturn(dummySettings);

            when(mock.updateSetting(anyString(), anyString())).thenReturn(true);
        });

        javafx.fxml.FXMLLoader fxmlLoader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
        fxmlLoader.setResources(App.getBundle());

        VBox root = fxmlLoader.load();
        controller = fxmlLoader.getController();

        stage.setScene(new Scene(root, 900, 600));
        stage.show();
    }

    @AfterEach
    public void tearDown() {
        if (mockedSettingsDAO != null)
            mockedSettingsDAO.close();
    }

    @Test
    public void testSettingsLoadAndSave(FxRobot robot) {
        WaitForAsyncUtils.waitForFxEvents(); // Wait for loadTask

        // Assert loaded values
        FxAssert.verifyThat("#storeNameField", TextInputControlMatchers.hasText("Test Store"));
        FxAssert.verifyThat("#currencyField", TextInputControlMatchers.hasText("USD"));

        // Modify fields
        robot.doubleClickOn("#taxRateField").write("8.0");

        // Click save button (.btn-primary)
        robot.clickOn(".btn-primary");

        WaitForAsyncUtils.waitForFxEvents(); // wait for saveTask

        // Verify save
        assertEquals(1, mockedSettingsDAO.constructed().size());
        SettingsDAO mockDao = mockedSettingsDAO.constructed().get(0);
        verify(mockDao, times(1)).updateSetting("store_name", "Test Store");
        verify(mockDao, times(1)).updateSetting("tax_rate", "8.0");
    }
}
