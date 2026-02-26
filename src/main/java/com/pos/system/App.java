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
    public void start(Stage stage) {
        try {
            // Init default locale
            String savedLang = com.pos.system.utils.ConfigManager.getLanguage();
            currentLocale = Locale.forLanguageTag(savedLang);
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
        } catch (Throwable t) {
            t.printStackTrace();
            try {
                java.nio.file.Path logPath = java.nio.file.Paths.get(System.getProperty("user.home"),
                        "StoreManager_startup_error.log");
                java.util.List<String> lines = new java.util.ArrayList<>();
                lines.add("App.start Error at " + java.time.LocalDateTime.now());
                lines.add(t.toString());
                for (StackTraceElement element : t.getStackTrace()) {
                    lines.add("  at " + element.toString());
                }
                java.nio.file.Files.write(logPath, lines, java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND);
            } catch (Exception e) {
                // Ignore log errors
            }
        }
    }

    public static void setRoot(String fxml) throws IOException {
        currentFxml = fxml;
        if (scene != null) {
            scene.setRoot(loadFXML(fxml));
            if (fxml.equals("dashboard")) {
                Stage stage = (Stage) scene.getWindow();
                stage.setWidth(1280);
                stage.setHeight(800);
                stage.centerOnScreen();
            }
        }
    }

    public static void setLocale(String language) {
        Locale newLocale = Locale.forLanguageTag(language);
        if (newLocale.equals(currentLocale))
            return; // No change, skip reload

        currentLocale = newLocale;
        com.pos.system.utils.ConfigManager.setLanguage(language);
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
