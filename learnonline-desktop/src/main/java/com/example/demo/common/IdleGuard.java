package com.example.demo.common;

import com.example.demo.Launcher;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.ApiClient;
import com.example.demo.common.TokenStore;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Global inactivity guard for the desktop client.
 *
 * <p>This utility installs listeners on a JavaFX {@link javafx.scene.Scene}
 * to detect user activity (mouse movement, key presses, etc.) and starts
 * an inactivity timer. If no interaction happens for the configured timeout,
 * the user is automatically logged out.</p>
 *
 * <p>When the timeout elapses, the guard:</p>
 * <ul>
 *   <li>Attempts to call the backend logout endpoint via {@link ApiClient}
 *       (typically {@code POST /api/auth/logout})</li>
 *   <li>Clears the stored JWT from {@link TokenStore}</li>
 *   <li>Shows a warning dialog using {@link AlertUtils} indicating that the
 *       session has expired due to inactivity</li>
 *   <li>Navigates back to the login screen using {@link Launcher}</li>
 * </ul>
 *
 * <p>The class is {@code final} and has a private constructor because it is
 * used purely as a static utility.</p>
 */

public final class IdleGuard {
    private IdleGuard() {}

    private static final ScheduledExecutorService EXEC = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "IdleGuard");
        t.setDaemon(true);
        return t;
    });

    private static volatile ScheduledFuture<?> task;
    private static volatile long timeoutMillis;
    private static volatile boolean firing;

    public static void attach(Scene scene, Duration idleTimeout) {
        if (scene == null || idleTimeout == null) return;
        timeoutMillis = idleTimeout.toMillis();

        Runnable reset = IdleGuard::resetTimer;

        scene.addEventFilter(MouseEvent.MOUSE_MOVED,   e -> reset.run());
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> reset.run());
        scene.addEventFilter(ScrollEvent.SCROLL,       e -> reset.run());
        scene.addEventFilter(KeyEvent.KEY_PRESSED,     e -> reset.run());
        scene.addEventFilter(TouchEvent.TOUCH_PRESSED, e -> reset.run());

        resetTimer();
    }

    private static synchronized void resetTimer() {
        if (task != null) task.cancel(false);
        task = EXEC.schedule(IdleGuard::fireLogout, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    private static void fireLogout() {

        if (!TokenStore.hasToken() || firing) return;

        firing = true;
        try {
            try { new ApiClient().post("/api/auth/logout", "{}"); } catch (Exception ignore) {}

            Platform.runLater(() -> {
                try { TokenStore.clear(); } catch (Throwable ignored) {}
                try { ErrorDialog.showWarning("Session expired due to inactivity. Please login again."); } catch (Throwable ignored) {}
                try { Launcher.go("login.fxml", "Login"); } catch (Throwable ignored) {}
            });
        } finally {
            firing = false;
        }
    }

}
