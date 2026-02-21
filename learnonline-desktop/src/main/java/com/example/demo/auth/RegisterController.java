package com.example.demo.auth;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.model.RegisterRequest;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * JavaFX controller for the user registration screen.
 *
 * <p>Collects account information (username, e-mail, password) and optional
 * profile details from the form, maps the chosen role to the backend enum
 * value and uses {@link AuthApi#register(com.example.demo.model.RegisterRequest)}
 * to create a new account. Also ensures role choices are available even when
 * not pre-populated in the FXML.</p>
 */

public class RegisterController {

    private final AuthApi authApi = new AuthApi();

    @FXML private TextField username;
    @FXML private TextField email;
    @FXML private PasswordField password;
    @FXML private PasswordField confirmPassword;
    @FXML private TextField firstName;
    @FXML private TextField lastName;
    @FXML private TextField phone;           // optional
    @FXML private ComboBox<String> role;     // "Student" | "Instructor"

    /**
     * Initializes the registration screen after the FXML has been loaded.
     *
     * <p>Typically used to populate the role {@link ComboBox} with values
     * such as "Student" and "Instructor" and to select a sensible default
     * when no value is chosen.</p>
     */

    @FXML
    private void initialize() {
        // make sure choices exist even if not set in FXML
        if (role != null && role.getItems().isEmpty()) {
            role.getItems().addAll("Student", "Instructor");
        }
        // optional: choose a sensible default
        if (role != null && role.getValue() == null) {
            role.setValue("Student");
        }
    }

    /**
     * Handles the registration form submission.
     *
     * <p>This method:</p>
     * <ul>
     *   <li>Reads and trims all fields from the UI</li>
     *   <li>Checks that required fields (username, email, password, confirm password)
     *       are not empty</li>
     *   <li>Validates that password and confirm password match</li>
     *   <li>Ensures that a role is selected</li>
     *   <li>Maps the chosen role label (e.g. "Student", "Instructor") to the
     *       backend enum value via {@link #mapRoleToEnum(String)}</li>
     *   <li>Builds a {@link RegisterRequest} and calls {@link AuthApi#register}</li>
     *   <li>On success, shows a confirmation message and navigates back to the
     *       login screen</li>
     * </ul>
     */

    @FXML
    private void onRegister() {
        try {
            String u  = safe(username);
            String e  = safe(email);
            String p  = safe(password);
            String cp = safe(confirmPassword);
            String f  = safe(firstName);
            String l  = safe(lastName);
            String ph = safe(phone);
            String r  = (role == null || role.getValue() == null) ? "" : role.getValue().trim();

            // required checks
            if (u.isEmpty() || e.isEmpty() || p.isEmpty() || cp.isEmpty()) {
                AlertUtils.warn("Fill all required fields (username, email, password, confirm).");
                return;
            }
            if (!p.equals(cp)) {
                AlertUtils.warn("Passwords do not match.");
                return;
            }
            if (r.isEmpty()) {
                AlertUtils.warn("Please choose a role (Student or Instructor).");
                return;
            }

            // map to backend enum values
            String roleEnum = mapRoleToEnum(r); // STUDENT | INSTRUCTOR

            RegisterRequest req = new RegisterRequest(
                    u,           // username
                    e,           // email
                    p,           // password
                    emptyToNull(f),
                    emptyToNull(l),
                    emptyToNull(ph),
                    roleEnum     // role must be enum string
            );

            authApi.register(req);
            AlertUtils.info("Account created. Please login.");
            Launcher.go("login.fxml", "Login");
        } catch (ApiException ex) {
            ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
        } catch (Exception ex) {
            ErrorDialog.showError(ex.getMessage());
        }
    }

    /**
     * Navigates from the registration screen back to the login screen.
     */

    @FXML
    private void goLogin() {
        Launcher.go("login.fxml", "Login");
    }

    /**
     * Safely reads and trims the text from a {@link TextField}.
     *
     * @param t the text field; may be {@code null}
     * @return a trimmed string, or {@code ""} if the field or its text is {@code null}
     */

    private static String safe(TextField t) {
        return (t == null || t.getText() == null) ? "" : t.getText().trim();
    }

    /**
     * Safely reads and trims the text from a {@link PasswordField}.
     *
     * @param t the password field; may be {@code null}
     * @return a trimmed string, or {@code ""} if the field or its text is {@code null}
     */

    private static String safe(PasswordField t) {
        return (t == null || t.getText() == null) ? "" : t.getText().trim();
    }

    /**
     * Converts an empty or blank string to {@code null}.
     *
     * @param s the string to normalize
     * @return {@code null} if {@code s} is {@code null} or blank; otherwise {@code s}
     */

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    /**
     * Maps the role label chosen in the UI to the backend enum value.
     *
     * @param r role label from the {@link ComboBox} (for example "Student" or "Instructor")
     * @return {@code "INSTRUCTOR"} if the label equals (ignoring case) "instructor";
     *         {@code "STUDENT"} otherwise
     */

    private static String mapRoleToEnum(String r) {
        return r.equalsIgnoreCase("instructor") ? "INSTRUCTOR" : "STUDENT";
    }
}
