package com.example.demo.student;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.dashboard.DashboardApi;
import com.example.demo.model.*;
import com.example.demo.quiz.QuizApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.concurrent.CompletableFuture;

/**
 * Student dashboard showing enrollments, progress and quizzes.
 *
 * <p>Loads the student's dashboard payload, filters upcoming quizzes the user
 * hasn't taken, and provides shortcuts to course detail, quiz taking, and
 * visualization.</p>
 */
public class StudentDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label enrolledCountLabel;
    @FXML private Label upcomingCountLabel;
    @FXML private Label lastScoreLabel;
    @FXML private Button visualizeButton;

    @FXML private TableView<CourseDto> enrolledTable;
    @FXML private TableColumn<CourseDto, String> courseTitleColumn;
    @FXML private TableColumn<CourseDto, Void> courseActionColumn;

    @FXML private TableView<QuizSummaryDto> quizTable;
    @FXML private TableColumn<QuizSummaryDto, String> quizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> quizQuestionColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> quizActionColumn;

    private final ObservableList<CourseDto> enrolledCourses = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> upcomingQuizzes = FXCollections.observableArrayList();
    private final DashboardApi dashboardApi = new DashboardApi();
    private final AuthApi authApi = new AuthApi();
    private final QuizApi quizApi = new QuizApi();
    private MeResponse me;
    private StudentDashboardResponse lastDashboard;

    /**
     * Configures tables/actions and kicks off profile + dashboard load.
     */
    @FXML
    public void initialize() {
        courseTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        courseActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            {
                openBtn.getStyleClass().add("primary-button");
                openBtn.setOnAction(e -> {
                    CourseDto c = getTableView().getItems().get(getIndex());
                    onOpenCourse(c);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : openBtn);
            }
        });
        enrolledTable.setItems(enrolledCourses);
        TableUtils.style(enrolledTable, courseTitleColumn, courseActionColumn);

        quizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quizQuestionColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
        quizActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button takeBtn = new Button("Take Quiz");
            {
                takeBtn.getStyleClass().add("secondary-button");
                takeBtn.setOnAction(e -> onTakeQuiz(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : takeBtn);
            }
        });
        quizTable.setItems(upcomingQuizzes);
        TableUtils.style(quizTable, quizNameColumn, quizQuestionColumn, quizActionColumn);

        loadMeAndDashboard();
    }

    /**
     * Loads the current user (cached or via API) then dashboard data.
     */
    private void loadMeAndDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                MeResponse cached = SessionStore.getMe();
                me = cached != null ? cached : authApi.me();
                if (cached == null) SessionStore.setMe(me);
                Platform.runLater(this::updateVisualizeButton);
                loadDashboard();
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load profile: " + ex.getMessage()));
            }
        });
    }

    /**
     * Fetches the student dashboard payload and populates the view.
     */
    private void loadDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                StudentDashboardResponse resp = dashboardApi.studentDashboard();
                Platform.runLater(() -> populate(resp));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load dashboard: " + ex.getMessage()));
            }
        });
    }

    /**
     * Populates labels/tables with dashboard data and updates counts/buttons.
     */
    private void populate(StudentDashboardResponse resp) {
        this.lastDashboard = resp;
        if (me != null) {
            welcomeLabel.setText("Welcome, " + (me.getFirstName() != null ? me.getFirstName() : me.getUsername()));
        }
        enrolledCourses.setAll(resp.getEnrolledCourses() != null ? resp.getEnrolledCourses() : java.util.List.of());
        upcomingQuizzes.setAll(resp.getUpcomingQuizzes() != null ? resp.getUpcomingQuizzes() : java.util.List.of());
        enrolledCountLabel.setText(String.valueOf(resp.getEnrolledCourseCount()));
        filterUpcomingByResult();
        if (resp.getRecentQuizResults() != null && !resp.getRecentQuizResults().isEmpty()) {
            QuizResultDto last = resp.getRecentQuizResults().get(0);
            lastScoreLabel.setText(last.getScore() + "/" + last.getTotalQuestions());
        } else {
            lastScoreLabel.setText("-");
        }
        updateVisualizeButton();
    }

    /**
     * Removes quizzes the student already completed from the upcoming list.
     */
    private void filterUpcomingByResult() {
        var items = new java.util.ArrayList<>(upcomingQuizzes);
        CompletableFuture.runAsync(() -> {
            java.util.List<QuizSummaryDto> remaining = new java.util.ArrayList<>();
            for (QuizSummaryDto q : items) {
                try {
                    QuizResultDto r = quizApi.myResult(q.getId());
                    if (r == null) {
                        remaining.add(q);
                    }
                } catch (Exception ex) {
                    // if API fails, keep it visible
                    remaining.add(q);
                }
            }
            Platform.runLater(() -> {
                upcomingQuizzes.setAll(remaining);
                upcomingCountLabel.setText(String.valueOf(remaining.size()));
            });
        });
    }

    private void onOpenCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    /**
     * Opens the graphics playground if the user has data to visualize.
     */
    @FXML
    private void onOpenGraphics() {
        if (!isStudentOrAdmin()) return;
        Launcher.go("graphics/graphics_playground.fxml", "Visualize Progress");
    }

    private void onTakeQuiz(QuizSummaryDto quiz) {
        SessionStore.setActiveQuiz(quiz);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    @FXML
    private void onOpenCatalog() { Launcher.go("course_catalog.fxml", "Course Catalog"); }

    @FXML
    public void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }

    private void updateVisualizeButton() {
        if (visualizeButton == null) return;
        boolean allowed = isStudentOrAdmin() && me != null;
        boolean hasData = lastDashboard != null
                && lastDashboard.getRecentQuizResults() != null
                && !lastDashboard.getRecentQuizResults().isEmpty();
        visualizeButton.setVisible(allowed);
        visualizeButton.setManaged(allowed);
        visualizeButton.setDisable(!hasData);
    }

    private boolean isStudentOrAdmin() {
        String role = SessionStore.currentRole();
        return "STUDENT".equals(role) || "ADMIN".equals(role);
    }
}
