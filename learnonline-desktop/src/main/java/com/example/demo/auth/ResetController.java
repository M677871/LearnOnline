package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.model.ResetPasswordRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the "reset password" screen.
 *
 * <p>This screen is shown after the user has requested a password-reset code.
 * It allows the user to enter the code received by e-mail and a new password,
 * and then calls the backend to complete the reset.</p>
 */

public class ResetController {
    private final AuthApi authApi = new AuthApi();
    String email;
    @FXML private TextField code;
    @FXML private PasswordField newPassword;

    /**
     * Initializes the controller after the FXML has been loaded.
     *
     * <p>Uses {@link javafx.application.Platform#runLater(Runnable)} to copy
     * any previously stored e-mail from {@link com.example.demo.auth.TempAuth#resetEmail}
     * into the {@code email} field, if available.</p>
     */

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            if (TempAuth.resetEmail != null && !TempAuth.resetEmail.isBlank()) {
                email = (TempAuth.resetEmail);
            }
        });
    }

    /**
     * Handles the reset password action.
     *
     * <p>Reads the reset code and new password from the UI, checks that both
     * are non-empty, and calls
     * {@link com.example.demo.auth.AuthApi#resetWithCode(com.example.demo.model.ResetPasswordRequest)}
     * with a {@link ResetPasswordRequest} built from the stored e-mail and
     * entered values.</p>
     *
     * <p>On success, an information dialog is shown and the user is
     * redirected to the login screen.</p>
     */

    public void onReset() {
        try {

            String c = safe(code.getText());
            String p = safe(newPassword.getText());

            if ( c.isEmpty() || p.isEmpty()) {
                AlertUtils.warn("Enter The code you received, and a new password.");
                return;
            }

            if (email == null || email.isBlank()) {
                AlertUtils.warn("Missing e-mail. Please restart the reset flow.");
                return;
            }

            authApi.resetWithCode(new ResetPasswordRequest(email, c, p));
            AlertUtils.info("Password reset. Please login with your new password.");
            Launcher.go("login.fxml", "Login");
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("Reset failed: " + ex.getMessage());
        }
    }

    /**
     * Simple utility to trim a possibly {@code null} string.
     *
     * @param s the string to normalize
     * @return {@code ""} if {@code s} is {@code null}; otherwise {@code s.trim()}
     */

    private static String safe(String s){ return s == null ? "" : s.trim(); }

    /**
     * Navigates back from the reset screen to the login screen.
     */

    public void goLogin(){ Launcher.go("login.fxml", "Login"); }
}
