package com.example.demo;

import com.example.demo.common.IdleGuard;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.net.URL;
import java.time.Duration;
import java.util.Objects;

/**
 * Central navigation helper that loads FXML screens, applies global styles,
 * and reuses a single JavaFX scene.
 */
public final class Launcher {

    private static Stage stage;
    private static Scene scene; // reuse one scene
    private static final String FXML_PREFIX = "/com/example/demo/fxml/";
    private static final String STYLES_PATH = "/com/example/demo/styles.css";
    private static final String ICON_PATH   = "/com/example/demo/icon.png"; // optional

    private static volatile boolean idleAttached; // ensure guard attaches only once

    private Launcher() {}

    /**
     * Initializes the launcher with the primary stage and optional app icon.
     */
    public static void init(Stage primary) {
        stage = Objects.requireNonNull(primary, "primary stage is null");

        // Try to set an app icon if present (non-fatal if missing)
        URL iconUrl = HelloApplication.class.getResource(ICON_PATH);
        if (iconUrl != null) {
            try { stage.getIcons().add(new Image(iconUrl.toExternalForm())); } catch (Exception ignored) {}
        }
    }

    /**
     * Navigates to the given FXML view, setting the window title and applying
     * shared styles. Can be called from any thread.
     */
    public static void go(String fxmlFileName, String title) {
        Runnable task = () -> {
            try {
                ensureInitialized();

                URL url = resolveFXML(fxmlFileName);
                if (url == null) {
                    throw new IllegalStateException("FXML not found: " + fxmlFileName);
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();

                if (scene == null) {
                    scene = new Scene(root);
                    applyGlobalStyles(scene);
                    stage.setScene(scene);
                } else {
                    scene.setRoot(root);
                }

                // Attach IdleGuard once to the reusable scene
                attachIdleGuardOnce(stage.getScene());

                if (title != null && !title.isBlank()) {
                    stage.setTitle(title);
                }

                if (!stage.isShowing()) stage.show();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to load " + fxmlFileName + ": " + ex.getMessage(), ex);
            }
        };

        if (Platform.isFxApplicationThread()) task.run(); else Platform.runLater(task);
    }

    /**
     * Resolves an FXML resource path (supports nested folders like graphics/).
     */
    private static URL resolveFXML(String name) {
        URL url = HelloApplication.class.getResource(FXML_PREFIX + name);
        if (url != null) return url;
        // Fallback to other resource roots (e.g., graphics/)
        String normalized = name.startsWith("/") ? name : "/com/example/demo/" + name;
        return HelloApplication.class.getResource(normalized);
    }

    private static void ensureInitialized() {
        if (stage == null) {
            throw new IllegalStateException("Launcher.init(stage) must be called before Launcher.go(...)");
        }
    }

    private static void applyGlobalStyles(Scene s) {
        URL css = HelloApplication.class.getResource(STYLES_PATH);
        if (css != null) {
            String uri = css.toExternalForm();
            if (!s.getStylesheets().contains(uri)) {
                s.getStylesheets().add(uri);
            }
        }
    }

    private static synchronized void attachIdleGuardOnce(Scene s) {
        if (!idleAttached && s != null) {
            IdleGuard.attach(s, Duration.ofMinutes(15));
            idleAttached = true;
        }
    }
}
