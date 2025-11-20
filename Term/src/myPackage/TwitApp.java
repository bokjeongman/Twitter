package myPackage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import myPackage.controllers.LoginController;
import myPackage.controllers.MainController;

import java.io.IOException;

public class TwitApp extends Application {

    private Stage primaryStage;
    private TwitService twitService;
    // Path to the CSS file in the resources folder
    private String cssPath = "/resources/styles.css"; 

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.twitService = new TwitService(); // Create the service
        showLoginScreen();
    }

    // 1. Show Login Screen
    public void showLoginScreen() {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/login.fxml"));
            Parent root = loader.load();

            // Pass the app and service to the controller
            LoginController controller = loader.getController();
            controller.setApp(this, twitService);

            primaryStage.setTitle("Twitter - Login");
            Scene scene = new Scene(root, 450, 350);
            
            // (Important) Apply CSS to the scene
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 2. Show Main Screen (after login)
    public void showMainScreen(String loggedInUserId) {
        try {
            // Load FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/main.fxml"));
            Parent root = loader.load();

            // Pass app, service, and UserID to the main controller
            MainController controller = loader.getController();
            controller.setApp(this, twitService, loggedInUserId);

            primaryStage.setTitle("Twitter - " + loggedInUserId); // Show user in title
            Scene scene = new Scene(root, 600, 700); // Larger window
            
             // (Important) Apply CSS to the scene
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Main method to launch the app
    public static void main(String[] args) {
        launch(args);
    }
}
