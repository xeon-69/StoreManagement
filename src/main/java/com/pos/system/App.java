package com.pos.system;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * JavaFX App
 */
public class App extends Application {

    private static Scene scene;
    private static Locale currentLocale;
    private static ResourceBundle bundle;
    private static String currentFxml;

    @Override
    public void start(Stage stage) throws IOException {
        // Init default locale
        currentLocale = Locale.forLanguageTag("en");
        bundle = ResourceBundle.getBundle("bundle.messages", currentLocale);

        // Apply AtlantaFX Theme
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        // We will start with the Login Screen
        currentFxml = "login";
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
        currentFxml = fxml;
        if (scene != null) {
            scene.setRoot(loadFXML(fxml));
        }
    }

    public static void setLocale(String language) {
        currentLocale = Locale.forLanguageTag(language);
        bundle = ResourceBundle.getBundle("bundle.messages", currentLocale);

        try {
            // Reload the current scene with the new bundle
            if (scene != null && currentFxml != null) {
                scene.setRoot(loadFXML(currentFxml));
            }
        } catch (IOException e) {
            // Error handled by caller if needed
        }
    }

    public static ResourceBundle getBundle() {
        return bundle;
    }

    private static Parent loadFXML(String fxml) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(App.class.getResource("/fxml/" + fxml + ".fxml"));
        fxmlLoader.setResources(bundle);
        return fxmlLoader.load();
    }

    public static void main(String[] args) {
        launch();
    }
}
