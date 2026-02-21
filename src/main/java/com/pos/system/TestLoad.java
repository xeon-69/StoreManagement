package com.pos.system;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.stage.Stage;

import java.util.ResourceBundle;

public class TestLoad extends Application {
    @Override
    public void start(Stage stage) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            loader.setResources(ResourceBundle.getBundle("bundle.messages"));
            Parent root = loader.load();
            System.out.println("SUCCESS: Settings loaded");
        } catch (Exception e) {
            System.err.println("FAILED: Settings");
            e.printStackTrace();
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/users.fxml"));
            loader.setResources(ResourceBundle.getBundle("bundle.messages"));
            Parent root = loader.load();
            System.out.println("SUCCESS: Users loaded");
        } catch (Exception e) {
            System.err.println("FAILED: Users");
            e.printStackTrace();
        }
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
