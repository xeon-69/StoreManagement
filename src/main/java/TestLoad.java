import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;
import java.util.ResourceBundle;
import java.util.Locale;

public class TestLoad extends Application {
    @Override
    public void start(Stage stage) {
        try {
            System.out.println("Attempting to load shift_management.fxml...");
            FXMLLoader loader = new FXMLLoader(TestLoad.class.getResource("/fxml/shift_management.fxml"));
            ResourceBundle bundle = ResourceBundle.getBundle("bundle.messages", Locale.forLanguageTag("en"));
            loader.setResources(bundle);
            loader.load();
            System.out.println("Success Loading FXML!");
        } catch (Exception e) {
            System.out.println("Exception loading FXML:");
            e.printStackTrace();
        } finally {
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
