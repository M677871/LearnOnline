package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.OtpVerifyRequest;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the OTP verification screen shown after login.
 *
 * <p>Allows the user to verify a one-time code sent by e-mail, resend the
 * code if needed, and routes the user to the appropriate dashboard when the
 * backend confirms authentication.</p>
 */
public class OtpController {
    private final AuthApi authApi = new AuthApi();
    @FXML private TextField code;

    /**
     * Verifies the OTP entered by the user.
     *
     * <p>Validates that a code and cached username are present, calls
     * {@link AuthApi#verifyOtp(com.example.demo.model.OtpVerifyRequest)}, and
     * navigates the user to a dashboard based on their role when successful.</p>
     */
    public void onVerify() {
        try {
            String c = code.getText() == null ? "" : code.getText().trim();
            if (c.isEmpty()) { AlertUtils.warn("Enter the OTP code."); return; }
            if (TempAuth.username == null || TempAuth.username.isBlank()) {
                ErrorDialog.showError("No username in memory. Please login again.");
                Launcher.go("login.fxml", "Login");
                return;
            }

            AuthResponse res = authApi.verifyOtp(new OtpVerifyRequest(TempAuth.username, c));

            String token = res.getToken();
            if (token == null || token.isBlank()) {
                ErrorDialog.showError("OTP verified, but no token was returned. Please try again.");
                return; // stay on OTP screen so user can retry/resend
            }

            AlertUtils.info("OTP verified.");
            navigateByRole(res.getRole());

        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("OTP verify failed: " + ex.getMessage());
        }
    }

    /**
     * Resends an OTP for the cached username in {@link TempAuth}.
     */
    public void onResend() {
        try {
            if (TempAuth.username == null || TempAuth.username.isBlank()) { AlertUtils.warn("Login again to resend."); return; }
            authApi.requestOtp(TempAuth.username);
            AlertUtils.info("OTP resent.");
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("Failed to resend: " + ex.getMessage());
        }
    }

    /**
     * Navigates back to the login screen.
     */
    public void goLogin(){ Launcher.go("login.fxml", "Login"); }

    /**
     * Opens the dashboard screen that matches the authenticated user's role.
     */
    private void navigateByRole(String role) {
        if (role != null && role.equalsIgnoreCase("ADMIN")) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else if (role != null && role.equalsIgnoreCase("INSTRUCTOR")) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }
}
