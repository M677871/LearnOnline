package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.model.CourseDto;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.User;
import com.example.demo.student.EnrollmentApi;
import com.example.demo.admin.UserApi;
import com.example.demo.course.CourseApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.paint.Color;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import com.example.demo.common.TableUtils;

/**
 * Admin view to inspect a student's enrollments.
 */
public class AdminEnrollmentsController {
    @FXML private TextField studentIdentifierField;
    @FXML private Button loadStudentButton;
    @FXML private Label studentStatusLabel;

    @FXML private Button loadInstructorButton;
    @FXML private Label instructorStatusLabel;

    @FXML private TableView<EnrollmentResponse> enrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, Long> courseIdColumn;
    @FXML private TableColumn<EnrollmentResponse, String> courseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> studentNameColumn;
    @FXML private TableColumn<EnrollmentResponse, String> studentEmailColumn;
    @FXML private TableColumn<EnrollmentResponse, String> statusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrolledAtColumn;

    @FXML private TableView<CourseDto> instructorCourseTable;
    @FXML private TableColumn<CourseDto, Long> instructorCourseIdColumn;
    @FXML private TableColumn<CourseDto, String> instructorCourseTitleColumn;
    @FXML private TableColumn<CourseDto, Boolean> instructorCoursePublishedColumn;
    @FXML private ComboBox<User> instructorPicker;
    @FXML private TextField courseNameField;
    @FXML private Button loadCourseEnrollmentsButton;

    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final UserApi userApi = new UserApi();
    private final CourseApi courseApi = new CourseApi();
    private final ObservableList<EnrollmentResponse> enrollments = FXCollections.observableArrayList();
    private final ObservableList<CourseDto> instructorCourses = FXCollections.observableArrayList();
    private final ObservableList<User> instructors = FXCollections.observableArrayList();

    @FXML
    /**
     * Initializes table bindings, cell factories, and loads instructor options.
     */
    public void initialize() {
        courseIdColumn.setCellValueFactory(new PropertyValueFactory<>("courseId"));
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
        studentNameColumn.setCellValueFactory(new PropertyValueFactory<>("studentUsername"));
        studentEmailColumn.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        enrolledAtColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
        enrollmentTable.setItems(enrollments);
        TableUtils.style(enrollmentTable, courseIdColumn, courseTitleColumn, studentNameColumn, studentEmailColumn, statusColumn, enrolledAtColumn);

        instructorCourseIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        instructorCourseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        instructorCoursePublishedColumn.setCellValueFactory(new PropertyValueFactory<>("published"));
        instructorCourseTable.setItems(instructorCourses);
        TableUtils.style(instructorCourseTable, instructorCourseIdColumn, instructorCourseTitleColumn, instructorCoursePublishedColumn);

        instructorPicker.setItems(instructors);
        instructorPicker.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername() + " (" + item.getEmail() + ")");
                setTextFill(Color.WHITE);
            }
        });
        instructorPicker.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getUsername());
                setTextFill(Color.WHITE);
            }
        });

        loadInstructors();
        hookButtonStates();
    }

    @FXML
    /**
     * Loads enrollments for the student identified by username or email.
     */
    private void onLoadStudent() {
        String identifier = studentIdentifierField.getText() == null ? "" : studentIdentifierField.getText().trim();
        if (identifier.isEmpty()) { AlertUtils.warn("Enter a username or email."); return; }

        loadStudentButton.setDisable(true);
        studentStatusLabel.setText("Loading...");
        CompletableFuture.runAsync(() -> {
            try {
                Long studentId = userApi.findByIdentifier(identifier)
                        .map(com.example.demo.model.User::getId)
                        .orElse(null);
                if (studentId == null) {
                    Platform.runLater(() -> {
                        studentStatusLabel.setText("User not found");
                        enrollments.clear();
                    });
                    return;
                }
                EnrollmentResponse[] resp = enrollmentApi.listByStudent(studentId);
                Platform.runLater(() -> {
                    enrollments.setAll(resp != null ? Arrays.asList(resp) : java.util.List.of());
                    studentStatusLabel.setText(enrollments.size() + " enrollments");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load enrollments: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadStudentButton.setDisable(false));
            }
        });
    }

    @FXML
    /**
     * Loads courses for the selected instructor into the table.
     */
    private void onLoadInstructor() {
        User instructor = instructorPicker.getSelectionModel().getSelectedItem();
        if (instructor == null) { AlertUtils.warn("Select an instructor."); return; }

        loadInstructorButton.setDisable(true);
        instructorStatusLabel.setText("Loading...");
        CompletableFuture.runAsync(() -> {
            try {
                CourseDto[] resp = courseApi.listInstructorCourses(instructor.getId());
                Platform.runLater(() -> {

                    instructorCourses.setAll(resp != null ? Arrays.asList(resp) : java.util.List.of());
                    instructorStatusLabel.setText(instructorCourses.size() + " courses");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load courses: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadInstructorButton.setDisable(false));
            }
        });
    }

    @FXML
    /**
     * Loads enrollments for a specific course title under the selected instructor.
     */
    private void onLoadCourseEnrollments() {
        User instructor = instructorPicker.getSelectionModel().getSelectedItem();
        String courseName = courseNameField.getText() == null ? "" : courseNameField.getText().trim();
        if (instructor == null) { AlertUtils.warn("Select an instructor."); return; }
        if (courseName.isBlank()) { AlertUtils.warn("Enter a course name."); return; }

        loadCourseEnrollmentsButton.setDisable(true);
        instructorStatusLabel.setText("Loading course enrollments...");
        CompletableFuture.runAsync(() -> {
            try {
                CourseDto[] courses = courseApi.listInstructorCourses(instructor.getId());
                CourseDto match = null;
                if (courses != null) {
                    for (CourseDto c : courses) {
                        if (c.getTitle() != null && c.getTitle().equalsIgnoreCase(courseName)) {
                            match = c; break;
                        }
                    }
                }
                if (match == null) {
                    Platform.runLater(() -> instructorStatusLabel.setText("Course not found for that instructor"));
                    return;
                }
                EnrollmentResponse[] resp = courseApi.listCourseEnrollments(match.getId());
                Platform.runLater(() -> {
                    enrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of());
                    instructorStatusLabel.setText(enrollments.size() + " users enrolled");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load course enrollments: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> loadCourseEnrollmentsButton.setDisable(false));
            }
        });
    }

    @FXML
    /**
     * Returns to the main admin dashboard screen.
     */
    private void backToDashboard() { Launcher.go("dashboard.fxml", "Dashboard"); }

    @FXML
    private void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }

    private void loadInstructors() {
        CompletableFuture.runAsync(() -> {
            try {
                var list = userApi.listInstructors(300);
                Platform.runLater(() -> instructors.setAll(list));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load instructors: " + ex.getMessage()));
            }
        });
    }

    private void hookButtonStates() {
        courseNameField.textProperty().addListener((obs, o, n) -> updateButtons());
        instructorPicker.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateButtons());
        updateButtons();
    }

    /**
     * Allows pressing Enter in the course name field to trigger the enabled
     * action (load enrollments when a course name is present, otherwise load
     * instructor courses).
     */
    @FXML
    private void onCourseNameEnter() {
        if (!loadCourseEnrollmentsButton.isDisable()) {
            onLoadCourseEnrollments();
        } else if (!loadInstructorButton.isDisable()) {
            onLoadInstructor();
        }
    }

    private void updateButtons() {
        String courseName = courseNameField.getText() == null ? "" : courseNameField.getText().trim();
        boolean hasCourseName = !courseName.isBlank();
        boolean hasInstructor = instructorPicker.getSelectionModel().getSelectedItem() != null;

        // Load Courses: enabled when no course name is provided (used to list courses)
        loadInstructorButton.setDisable(hasCourseName || !hasInstructor);
        // Load Enrollments: enabled when both instructor and course name are provided
        loadCourseEnrollmentsButton.setDisable(!(hasCourseName && hasInstructor));
    }
}
