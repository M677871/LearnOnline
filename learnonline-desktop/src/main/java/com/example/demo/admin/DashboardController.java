package com.example.demo.admin;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.CourseDetailDto;
import com.example.demo.model.CourseDto;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.model.QuizResultDto;
import com.example.demo.model.QuizSummaryDto;
import com.example.demo.student.EnrollmentApi;
import com.example.demo.course.CourseApi;
import com.example.demo.quiz.QuizApi;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.concurrent.CompletableFuture;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JavaFX controller for the main dashboard screen.
 *
 * <p>After login, this screen shows basic profile information about the
 * current user (name, e-mail and role) and provides navigation shortcuts
 * to other management screens such as users and categories.</p>
 */

public class DashboardController {
    private final AuthApi authApi = new AuthApi();

    @FXML private Label welcome;
    @FXML private Label name;
    @FXML private Label email;
    @FXML private Label role;
    @FXML private TableView<EnrollmentResponse> myEnrollmentsTable;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseStatusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseEnrolledAtColumn;
    @FXML private TableColumn<EnrollmentResponse, Void> myCourseActionColumn;
    @FXML private TableView<QuizSummaryDto> upcomingQuizTable;
    @FXML private TableColumn<QuizSummaryDto, String> upcomingCourseColumn;
    @FXML private TableColumn<QuizSummaryDto, String> upcomingQuizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> upcomingQuizQuestionsColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> upcomingQuizActionColumn;

    private final ObservableList<EnrollmentResponse> myEnrollments = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> upcomingQuizzes = FXCollections.observableArrayList();
    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final CourseApi courseApi = new CourseApi();
    private final QuizApi quizApi = new QuizApi();
    private final Map<Long, String> courseTitles = new ConcurrentHashMap<>();

    /**
     * Initializes the dashboard after the FXML has been loaded.
     *
     * <p>Using {@link Platform#runLater(Runnable)}, this method:</p>
     * <ul>
     *   <li>Checks that a JWT is present in {@link TokenStore}; if not, the
     *       user is redirected back to the login screen</li>
     *   <li>Calls {@link AuthApi#me()} to fetch the current user's profile</li>
     *   <li>Builds a display name from first and last name (or falls back to
     *       the username) and updates the welcome labels accordingly</li>
     * </ul>
     *
     * <p>If any error occurs while loading the profile, an error dialog is
     * shown to the user.</p>
     */

    @FXML
    public void initialize() {
        welcome.setText("Welcome");
        name.setText(""); email.setText(""); role.setText("");

        if (myEnrollmentsTable != null) {
            myCourseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("courseTitle"));
            myCourseStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
            myCourseEnrolledAtColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
            myCourseActionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button openBtn = new Button("Open");
                {
                    openBtn.getStyleClass().add("secondary-button");
                    openBtn.setOnAction(e -> {
                        EnrollmentResponse er = getTableView().getItems().get(getIndex());
                        openCourse(er);
                    });
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    setGraphic(empty ? null : openBtn);
                }
            });
            myEnrollmentsTable.setItems(myEnrollments);
            TableUtils.style(myEnrollmentsTable, myCourseTitleColumn, myCourseStatusColumn, myCourseEnrolledAtColumn, myCourseActionColumn);
        }

        if (upcomingQuizTable != null) {
            upcomingCourseColumn.setCellValueFactory(cell -> {
                QuizSummaryDto q = cell.getValue();
                String title = q != null && q.getCourseId() != null ? courseTitles.getOrDefault(q.getCourseId(), "") : "";
                return new javafx.beans.property.SimpleStringProperty(title);
            });
            upcomingQuizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
            upcomingQuizQuestionsColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
            upcomingQuizActionColumn.setCellFactory(col -> new TableCell<>() {
                private final Button takeBtn = new Button("Take");
                {
                    takeBtn.getStyleClass().add("primary-button");
                    takeBtn.setOnAction(e -> {
                        QuizSummaryDto quiz = getTableView().getItems().get(getIndex());
                        onTakeQuiz(quiz);
                    });
                }
                @Override protected void updateItem(Void v, boolean empty) {
                    super.updateItem(v, empty);
                    setGraphic(empty ? null : takeBtn);
                }
            });
            upcomingQuizTable.setItems(upcomingQuizzes);
            TableUtils.style(upcomingQuizTable, upcomingCourseColumn, upcomingQuizNameColumn, upcomingQuizQuestionsColumn, upcomingQuizActionColumn);
        }

        Platform.runLater(() -> {
            try {
                if (!TokenStore.hasToken()) {
                    AlertUtils.warn("No token present. Please login again.");
                    Launcher.go("login.fxml", "Login");
                    return;
                }
                MeResponse me = authApi.me(); // your tolerant/profile call
                SessionStore.setMe(me);
                String full = (me.getFirstName() == null ? "" : me.getFirstName());
                if (me.getLastName() != null && !me.getLastName().isBlank()) full += (full.isBlank() ? "" : " ") + me.getLastName();
                if (full.isBlank()) full = me.getUsername();

                welcome.setText(full.isBlank() ? "Welcome" : ("Welcome, " + full));
                name.setText(full);
                email.setText(me.getEmail());
                role.setText(me.getRole());
                loadMyEnrollments(me.getId());
                loadUpcomingQuizzes(me.getId());
            } catch (ApiException ex) {
                ErrorDialog.showError(ex.getMessage(), ex.getErrorCode());
            } catch (Exception ex) {
                ErrorDialog.showError("Failed to load profile: " + ex.getMessage());
            }
        });
    }

    private void loadMyEnrollments(Long userId) {
        if (userId == null || myEnrollmentsTable == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                var resp = enrollmentApi.listByCurrentUser(userId);
                Platform.runLater(() -> myEnrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load your enrollments: " + ex.getMessage()));
            }
        });
    }

    private void loadUpcomingQuizzes(Long userId) {
        if (userId == null || upcomingQuizTable == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                var enrollments = enrollmentApi.listByCurrentUser(userId);
                if (enrollments == null) enrollments = new EnrollmentResponse[0];
                courseTitles.clear();
                List<QuizSummaryDto> pending = new ArrayList<>();
                Set<Long> added = new HashSet<>();
                for (EnrollmentResponse er : enrollments) {
                    if (er.getCourseId() == null) continue;
                    try {
                        CourseDetailDto detail = courseApi.get(er.getCourseId());
                        if (detail == null) continue;
                        courseTitles.put(detail.getId(), detail.getTitle());
                        if (detail.getQuizzes() == null) continue;
                        for (QuizSummaryDto q : detail.getQuizzes()) {
                            if (q == null || q.getId() == null || added.contains(q.getId())) continue;
                            QuizResultDto r = null;
                            try { r = quizApi.myResult(q.getId()); } catch (Exception ignored) {}
                            if (r == null) {
                                pending.add(q);
                                added.add(q.getId());
                            }
                        }
                    } catch (Exception ignored) {}
                }
                Platform.runLater(() -> upcomingQuizzes.setAll(pending));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load upcoming quizzes: " + ex.getMessage()));
            }
        });
    }

    private void openCourse(EnrollmentResponse er) {
        if ( er == null || er.getCourseId() == null) return;
        CourseDto c = new CourseDto();
        c.setId(er.getCourseId());
        c.setTitle(er.getCourseTitle());
        SessionStore.setActiveCourse(c);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    /**
     * Navigates to the quiz-taking screen for the selected quiz.
     *
     * @param quiz the quiz to open
     */
    private void onTakeQuiz(QuizSummaryDto quiz) {
        if (quiz == null) return;
        SessionStore.setActiveQuiz(quiz);
        CourseDto c = new CourseDto();
        c.setId(quiz.getCourseId());
        c.setTitle(courseTitles.getOrDefault(quiz.getCourseId(), ""));
        SessionStore.setActiveCourse(c);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    /**
     * Opens the user management screen.
     */

    @FXML private void openUsers()      { Launcher.go("user_Dashboard.fxml", "Users"); }

    /**
     * Opens the category management screen.
     */

    @FXML private void openCategories() { Launcher.go("category.fxml", "Categories"); }
    @FXML private void openCourses()    { Launcher.go("course_catalog.fxml", "Courses"); }
    @FXML private void openEnrollments(){ Launcher.go("admin_enrollments.fxml", "Enrollments"); }
    @FXML private void openVisuals()    { Launcher.go("graphics/graphics_playground.fxml", "Data Visualizations"); }

    /**
     * Logs the user out by clearing the stored JWT and returning to the login screen.
     */

    @FXML
    public void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }
}
