package com.pos.system.controllers;

import com.pos.system.dao.ShiftDAO;
import com.pos.system.models.Shift;
import com.pos.system.models.User;
import com.pos.system.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ShiftController {

    private ShiftDAO shiftDAO;
    private com.pos.system.dao.SaleDAO saleDAO;

    public void setShiftDAO(ShiftDAO shiftDAO) {
        this.shiftDAO = shiftDAO;
    }

    public void setSaleDAO(com.pos.system.dao.SaleDAO saleDAO) {
        this.saleDAO = saleDAO;
    }

    protected ShiftDAO createShiftDAO() throws SQLException {
        return shiftDAO != null ? shiftDAO : new ShiftDAO();
    }

    protected com.pos.system.dao.SaleDAO createSaleDAO() throws SQLException {
        return saleDAO != null ? saleDAO : new com.pos.system.dao.SaleDAO();
    }

    @FXML
    private VBox openShiftContainer;
    @FXML
    private TextField openingCashField;
    @FXML
    private Button openShiftBtn;

    @FXML
    private VBox closeShiftContainer;
    @FXML
    private Label shiftInfoLabel;
    @FXML
    private TextField actualCashField;
    @FXML
    private Button closeShiftBtn;

    @FXML
    private Label errorLabel;

    @FXML
    public void initialize() {
        Shift currentShift = SessionManager.getInstance().getCurrentShift();
        if (currentShift == null) {
            openShiftContainer.setVisible(true);
            openShiftContainer.setManaged(true);
            closeShiftContainer.setVisible(false);
            closeShiftContainer.setManaged(false);
        } else {
            openShiftContainer.setVisible(false);
            openShiftContainer.setManaged(false);
            closeShiftContainer.setVisible(true);
            closeShiftContainer.setManaged(true);
            shiftInfoLabel.setText("Shift Started: " + currentShift.getStartTime().toString());
        }
    }

    @FXML
    private void handleOpenShift() {
        try {
            double openingCash = Double.parseDouble(openingCashField.getText());
            if (openingCash < 0)
                throw new NumberFormatException();

            User user = SessionManager.getInstance().getCurrentUser();
            Shift shift = new Shift();
            shift.setUserId(user.getId());
            shift.setOpeningCash(openingCash);
            shift.setStatus("OPEN");
            shift.setStartTime(LocalDateTime.now());

            try (ShiftDAO dao = createShiftDAO()) {
                shift = dao.create(shift);
                SessionManager.getInstance().setCurrentShift(shift);
                com.pos.system.App.setRoot("dashboard");
            }

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid positive number for opening cash.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleCloseShift() {
        try {
            double actualCash = Double.parseDouble(actualCashField.getText());
            if (actualCash < 0)
                throw new NumberFormatException();

            Shift currentShift = SessionManager.getInstance().getCurrentShift();
            currentShift.setActualClosingCash(actualCash);
            currentShift.setEndTime(LocalDateTime.now());
            currentShift.setStatus("CLOSED");

            // Normally expected cash is calculated dynamically from (opening + sales -
            // refunds +/- manual ins/outs)
            // For now we set it to 0 as a placeholder, or we calculate it.
            // Let's just calculate expected cash simply: opening cash + total sales.
            double expectedCash = currentShift.getOpeningCash();
            try (com.pos.system.dao.SaleDAO saleDao = createSaleDAO()) {
                double totalSales = saleDao.getTotalSalesBetween(currentShift.getStartTime(),
                        currentShift.getEndTime());
                expectedCash += totalSales;
            }
            currentShift.setExpectedClosingCash(expectedCash);

            try (ShiftDAO dao = createShiftDAO()) {
                dao.update(currentShift);
                SessionManager.getInstance().setCurrentShift(null);
                SessionManager.getInstance().logout();
                com.pos.system.App.setRoot("login");
            }

        } catch (NumberFormatException e) {
            errorLabel.setText("Please enter a valid positive number for actual cash.");
        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Database error: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            if (SessionManager.getInstance().getCurrentShift() != null) {
                com.pos.system.App.setRoot("dashboard");
            } else {
                SessionManager.getInstance().logout();
                com.pos.system.App.setRoot("login");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
