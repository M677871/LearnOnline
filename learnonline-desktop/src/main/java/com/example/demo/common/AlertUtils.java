package com.example.demo.common;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Small JavaFX utility class for showing information, warning and error dialogs.
 *
 * <p>The methods in this class make sure that dialogs are always created and
 * shown on the JavaFX application thread. If they are invoked from a
 * background thread, the call is forwarded using
 * {@link Platform#runLater(Runnable)}.</p>
 */
public final class AlertUtils {

    /**
     * Utility class; not meant to be instantiated.
     */
    private AlertUtils() {}

    /**
     * Shows an information dialog with the given message.
     *
     * @param msg the message to display
     */
    public static void info(String msg) {
        show(Alert.AlertType.INFORMATION, "Info", msg);
    }

    /**
     * Shows a warning dialog with the given message.
     *
     * @param msg the message to display
     */
    public static void warn(String msg) {
        show(Alert.AlertType.WARNING, "Warning", msg);
    }

    /**
     * Shows an error dialog with the given message.
     *
     * @param msg the message to display
     */
    public static void error(String msg) {
        show(Alert.AlertType.ERROR, "Error", msg);
    }

    /**
     * Internal helper that creates and shows an {@link Alert} of the given type.
     *
     * @param type  alert type to use
     * @param title window title for the dialog
     * @param msg   content text of the dialog
     */
    private static void show(Alert.AlertType type, String title, String msg) {
        Runnable r = () -> {
            Alert a = new Alert(type, msg, ButtonType.OK);
            a.setHeaderText(null);
            a.setTitle(title);
            a.show();
        };
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }
}
