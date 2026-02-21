package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.model.AuthResponse;
import com.example.demo.model.LoginRequest;
import com.example.demo.model.MeResponse;
import javafx.fxml.FXML;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the login screen of the desktop client.
 *
 * <p>This controller:</p>
 * <ul>
 *   <li>Reads credentials from the UI and authenticates via {@link AuthApi#login(com.example.demo.model.LoginRequest)}</li>
 *   <li>Handles OTP-required responses by caching the username in {@link TempAuth} and navigating to the OTP screen</li>
 *   <li>On normal login, relies on {@link com.example.demo.common.TokenStore} being set, fetches the profile via {@link AuthApi#me()}, caches it in {@link SessionStore}, and opens a role-based dashboard</li>
 * </ul>
 */

public class LoginController {
    private final AuthApi authApi = new AuthApi();
    @FXML private TextField username;
    @FXML private PasswordField password;

    /**
     * Handles the login button action.
     *
     * <p>Steps:</p>
     * <ul>
     *   <li>Validate that both username and password are provided</li>
     *   <li>Call {@link AuthApi#login(LoginRequest)} to authenticate</li>
     *   <li>If {@link AuthResponse#isOtpRequired()} is true, store {@link TempAuth#username} and route to the OTP screen</li>
     *   <li>Otherwise ensure a token exists, fetch the current user via {@link AuthApi#me()}, cache it in {@link SessionStore}, and navigate based on role</li>
     * </ul>
     */

    public void onLogin() {
        try {
            String u = username.getText() == null ? "" : username.getText().trim();
            String p = password.getText() == null ? "" : password.getText().trim();
            if (u.isEmpty() || p.isEmpty()) { AlertUtils.warn("Please enter username and password."); return; }

            AuthResponse res = authApi.login(new LoginRequest(u, p));

            if (res != null && res.isOtpRequired()) {
                TempAuth.username = u;
                AlertUtils.info("We've sent a 6-digit code to your email. Please check your inbox.");
                Launcher.go("otp.fxml", "Verify OTP");
                return;
            }

            if (res == null || res.getToken() == null || res.getToken().isBlank()) {
                ErrorDialog.showError("Login failed: token missing");
                return;
            }

            MeResponse me = authApi.me();
            SessionStore.setMe(me);
            navigateByRole(me.getRole());
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError("Login failed: " + ex.getMessage());
        }
    }
    /**
     * Navigates from the login screen to the registration screen.
     */

    public void goRegister(){ Launcher.go("register.fxml", "Register"); }

    /**
     * Navigates from the login screen to the "forgot password" screen.
     */

    public void goForgot(){ Launcher.go("forget.fxml", "Forgot Password"); }

    /**
     * Opens the dashboard corresponding to the authenticated user's role.
     *
     * @param role role string from the backend (e.g. ADMIN, INSTRUCTOR, STUDENT)
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
