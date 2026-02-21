package com.example.demo;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX entry point that boots the app and opens the login screen.
 */
public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) {
        // Initialize the global launcher with the primary stage
        Launcher.init(stage);

        stage.setMinWidth(850);
        stage.setMinHeight(550);

        Launcher.go("login.fxml", "Login");
    }

    public static void main(String[] args) {
        launch(args);
    }
}
