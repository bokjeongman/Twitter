package myPackage.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import myPackage.TwitApp;
import myPackage.TwitService;

public class LoginController {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private TwitApp twitApp;
    private TwitService twitService;

    public void setApp(TwitApp twitApp, TwitService twitService) {
        this.twitApp = twitApp;
        this.twitService = twitService;
    }

    @FXML
    protected void handleLogin() {
        String id = idField.getText();
        String pwd = passwordField.getText();

        if (id.isEmpty() || pwd.isEmpty()) {
            statusLabel.setText("Please enter ID and Password.");
            return;
        }

        String loggedInUserId = twitService.login(id, pwd);
        
        if (loggedInUserId != null) {
            // Login Success! Switch to main screen
            statusLabel.setText("Login Success!");
            twitApp.showMainScreen(loggedInUserId);
        } else {
            // Login Failed
            statusLabel.setText("Error: ID or Password is not correct.");
        }
    }

    @FXML
    protected void handleSignUp() {
        String id = idField.getText();
        String pwd = passwordField.getText();

        if (id.isEmpty() || pwd.isEmpty()) {
            statusLabel.setText("Please enter ID and Password.");
            return;
        }

        boolean success = twitService.signUp(id, pwd);

        if (success) {
            statusLabel.setText("Sign Up Success! Please log in.");
        } else {
            statusLabel.setText("Sign Up Failed (ID may already exist).");
        }
    }
}package myPackage.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import myPackage.TwitApp;
import myPackage.TwitService;

public class LoginController {

    @FXML private TextField idField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;

    private TwitApp twitApp;
    private TwitService twitService;

    public void setApp(TwitApp twitApp, TwitService twitService) {
        this.twitApp = twitApp;
        this.twitService = twitService;
    }

    @FXML
    protected void handleLogin() {
        String id = idField.getText();
        String pwd = passwordField.getText();

        if (id.isEmpty() || pwd.isEmpty()) {
            statusLabel.setText("Please enter ID and Password.");
            return;
        }

        String loggedInUserId = twitService.login(id, pwd);
        
        if (loggedInUserId != null) {
            // Login Success! Switch to main screen
            statusLabel.setText("Login Success!");
            twitApp.showMainScreen(loggedInUserId);
        } else {
            // Login Failed
            statusLabel.setText("Error: ID or Password is not correct.");
        }
    }

    @FXML
    protected void handleSignUp() {
        String id = idField.getText();
        String pwd = passwordField.getText();

        if (id.isEmpty() || pwd.isEmpty()) {
            statusLabel.setText("Please enter ID and Password.");
            return;
        }

        boolean success = twitService.signUp(id, pwd);

        if (success) {
            statusLabel.setText("Sign Up Success! Please log in.");
        } else {
            statusLabel.setText("Sign Up Failed (ID may already exist).");
        }
    }
}
