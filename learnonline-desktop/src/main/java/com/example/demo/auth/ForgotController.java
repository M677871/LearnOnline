package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the "forgot password" screen.
 *
 * <p>Accepts either an e-mail address or username, triggers the backend
 * password-reset flow via {@link AuthApi#requestResetOtp(String)}, remembers
 * the e-mail for convenience, and then navigates to the reset screen.</p>
 */

public class ForgotController {
    private final AuthApi authApi = new AuthApi();
    @FXML private TextField emailOrUsername;

    /**
     * Sends a password-reset request for the value entered in the text field.
     *
     * <p>If the field is empty, a warning dialog is shown. Otherwise
     * {@link com.example.demo.auth.AuthApi#requestResetOtp(String)} is called
     * and, when the value looks like an e-mail address, it is also stored in
     * {@link com.example.demo.auth.TempAuth#resetEmail} so that the reset
     * screen can prefill it.</p>
     *
     * <p>On success the user is navigated to the reset password screen.</p>
     */

    public void onSend(){
        try {
            String v = emailOrUsername.getText()==null? "": emailOrUsername.getText().trim();
            if (v.isEmpty()) { AlertUtils.warn("Enter your email or username."); return; }

            authApi.requestResetOtp(v);
            // If user typed an email, remember it to prefill reset screen
            if (v.contains("@")) TempAuth.resetEmail = v;

            AlertUtils.info("If the account exists, a reset code has been sent.");
            Launcher.go("reset.fxml", "Reset Password");
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("Failed to send reset link: " + ex.getMessage());
        }
    }

    /**
     * Navigates back from the "forgot password" screen to the login screen.
     */

    public void goLogin(){ Launcher.go("login.fxml", "Login"); }
}
