package myPackage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import myPackage.controllers.LoginController;
import myPackage.controllers.MainController;

import java.io.IOException;

public class TwitApp extends Application {

    private Stage primaryStage;
    private TwitService twitService;
    private String cssPath = "/resources/styles.css"; 

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;
        this.twitService = new TwitService();
        showLoginScreen();
    }

    public void showLoginScreen() {
        try {
            if (primaryStage != null) primaryStage.close();

            Stage loginStage = new Stage(); 
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/login.fxml"));
            Parent root = loader.load();

            LoginController controller = loader.getController();
            controller.setApp(this, twitService);

            loginStage.setTitle("Twitter - Login");
            Scene scene = new Scene(root, 450, 550); 
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            scene.setFill(Color.web("#15202B"));

            loginStage.setScene(scene);
            loginStage.setResizable(false); 
            loginStage.show();
            
            this.primaryStage = loginStage;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showMainScreen(String loggedInUserId) {
        try {
            if (primaryStage != null) primaryStage.close();

            Stage mainStage = new Stage();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/resources/main.fxml"));
            Parent root = loader.load();

            MainController controller = loader.getController();
            controller.setApp(this, twitService, loggedInUserId);

            mainStage.setTitle("Twitter - " + loggedInUserId);
            
            Scene scene = new Scene(root, 600, 900); 
            scene.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            scene.setFill(Color.web("#15202B"));

            mainStage.setScene(scene);
            mainStage.centerOnScreen();
            
            mainStage.setResizable(false);
            mainStage.show();
            mainStage.setResizable(true); 
            
            this.primaryStage = mainStage;

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
