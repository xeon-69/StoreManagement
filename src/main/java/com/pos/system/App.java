package com.pos.system;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        // Apply AtlantaFX Theme
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // We will start with the Login Screen
        scene = new Scene(loadFXML("login"), 640, 480);

        // Load CSS if available?
        // scene.getStylesheets().add(App.class.getResource("/css/styles.css").toExternalForm());

        stage.setScene(scene);
        stage.setTitle("Store Management System");
        stage.show();

        // Initialize Database
        com.pos.system.database.DatabaseManager.getInstance();

        // Start Background Services
        com.pos.system.services.StoreMonitorService monitorService = new com.pos.system.services.StoreMonitorService();
        monitorService.start();
    }

    public static void setRoot(String fxml) throws IOException {
        scene.setRoot(loadFXML(fxml));
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
