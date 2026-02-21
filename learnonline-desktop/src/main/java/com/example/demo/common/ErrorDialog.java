package com.example.demo.common;

import javafx.application.Platform;
import javafx.scene.control.Alert;

/**
 * Simple helper for showing consistent error dialogs.
 */
public final class ErrorDialog {
    private ErrorDialog() {}

    public static void showError(String message) {
        showError(message, null);
    }

    public static void showError(String message, String code) {
        String text = message == null ? "Something went wrong." : message;
        if (code != null && !code.isBlank()) {
            text = text + " (" + code + ")";
        }
        show(Alert.AlertType.ERROR, "Error", text);
    }

    public static void showInfo(String message) {
        show(Alert.AlertType.INFORMATION, "Info", message);
    }

    public static void showWarning(String message) {
        show(Alert.AlertType.WARNING, "Warning", message);
    }

    private static void show(Alert.AlertType type, String title, String message) {
        Runnable task = () -> {
            Alert alert = new Alert(type);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message == null ? "" : message);
            alert.showAndWait();
        };
        if (Platform.isFxApplicationThread()) task.run(); else Platform.runLater(task);
    }
}
