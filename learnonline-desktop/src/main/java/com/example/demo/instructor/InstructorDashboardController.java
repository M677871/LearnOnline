package com.example.demo.instructor;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.course.CourseApi;
import com.example.demo.model.EnrollmentResponse;
import com.example.demo.model.CourseDto;
import com.example.demo.model.InstructorDashboardResponse;
import com.example.demo.model.MeResponse;
import com.example.demo.model.CourseDetailDto;
import com.example.demo.model.QuizSummaryDto;
import com.example.demo.model.QuizResultDto;
import com.example.demo.dashboard.DashboardApi;
import com.example.demo.student.EnrollmentApi;
import com.example.demo.quiz.QuizApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell;
import javafx.scene.control.Button;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Instructor dashboard showing owned courses, enrollments, and upcoming quizzes,
 * with shortcuts to edit courses, open analytics, and take quizzes as a student.
 */
public class InstructorDashboardController {

    @FXML private Label welcomeLabel;
    @FXML private Label courseCountLabel;
    @FXML private Label enrollmentCountLabel;
    @FXML private Label lastCourseLabel;
    @FXML private Button analyticsButton;

    @FXML private TableView<CourseDto> courseTable;
    @FXML private TableColumn<CourseDto, String> titleColumn;
    @FXML private TableColumn<CourseDto, Boolean> publishedColumn;
    @FXML private TableColumn<CourseDto, String> createdColumn;
    @FXML private TableColumn<CourseDto, Void> actionsColumn;

    @FXML private TableView<EnrollmentResponse> enrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, String> enrollStudentColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollEmailColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollStatusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> enrollDateColumn;

    @FXML private TableView<EnrollmentResponse> myEnrollmentTable;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseTitleColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseStatusColumn;
    @FXML private TableColumn<EnrollmentResponse, String> myCourseEnrolledAtColumn;
    @FXML private TableColumn<EnrollmentResponse, Void> myCourseActionColumn;

    @FXML private TableView<QuizSummaryDto> upcomingQuizTable;
    @FXML private TableColumn<QuizSummaryDto, String> upcomingCourseColumn;
    @FXML private TableColumn<QuizSummaryDto, String> upcomingQuizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> upcomingQuizQuestionsColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> upcomingQuizActionColumn;

    private final ObservableList<CourseDto> courses = FXCollections.observableArrayList();
    private final ObservableList<EnrollmentResponse> enrollments = FXCollections.observableArrayList();
    private final ObservableList<EnrollmentResponse> myEnrollments = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> upcomingQuizzes = FXCollections.observableArrayList();
    private final CourseApi courseApi = new CourseApi();
    private final DashboardApi dashboardApi = new DashboardApi();
    private final AuthApi authApi = new AuthApi();
    private final EnrollmentApi enrollmentApi = new EnrollmentApi();
    private final QuizApi quizApi = new QuizApi();
    private MeResponse me;
    private final Map<Long, String> courseTitles = new HashMap<>();
    private InstructorDashboardResponse lastDashboard;

    /**
     * Configures table columns/actions and kicks off loading of profile,
     * dashboard stats, enrollments, and quizzes.
     */
    @FXML
    public void initialize() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        publishedColumn.setCellValueFactory(new PropertyValueFactory<>("published"));
        publishedColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Boolean v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : (Boolean.TRUE.equals(v) ? "Published" : "Draft"));
            }
        });
        createdColumn.setCellValueFactory(c -> {
            if (c.getValue().getCreatedAt() == null) return null;
            String formatted = DateTimeFormatter.ISO_INSTANT.format(c.getValue().getCreatedAt());
            return new javafx.beans.property.SimpleStringProperty(formatted);
        });

        actionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button openBtn = new Button("Open");
            private final Button editBtn = new Button("Edit");
            {
                openBtn.getStyleClass().add("ghost-button");
                editBtn.getStyleClass().add("primary-button");
                openBtn.setOnAction(e -> onOpenCourse(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> onEditCourse(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                var box = new javafx.scene.layout.HBox(8, openBtn, editBtn);
                setGraphic(box);
            }
        });
        courseTable.setItems(courses);
        TableUtils.style(courseTable, titleColumn, publishedColumn, createdColumn, actionsColumn);

        enrollStudentColumn.setCellValueFactory(new PropertyValueFactory<>("studentUsername"));
        enrollEmailColumn.setCellValueFactory(new PropertyValueFactory<>("studentEmail"));
        enrollStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        enrollDateColumn.setCellValueFactory(new PropertyValueFactory<>("enrolledAt"));
        enrollmentTable.setItems(enrollments);
        TableUtils.style(enrollmentTable, enrollStudentColumn, enrollEmailColumn, enrollStatusColumn, enrollDateColumn);

        if (myEnrollmentTable != null) {
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
            myEnrollmentTable.setItems(myEnrollments);
            TableUtils.style(myEnrollmentTable, myCourseTitleColumn, myCourseStatusColumn, myCourseEnrolledAtColumn, myCourseActionColumn);
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

        loadMeAndDashboard();
    }

    /**
     * Loads current user (from cache or API) then triggers dashboard data loads.
     */
    private void loadMeAndDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                MeResponse cached = SessionStore.getMe();
                me = cached != null ? cached : authApi.me();
                if (cached == null) SessionStore.setMe(me);
                Platform.runLater(this::updateAnalyticsButton);
                loadMyEnrollments();
                loadUpcomingQuizzes();
                loadDashboard();
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load profile: " + ex.getMessage()));
            }
        });
    }

    /**
     * Fetches instructor dashboard summary and populates the view.
     */
    private void loadDashboard() {
        CompletableFuture.runAsync(() -> {
            try {
                InstructorDashboardResponse resp = dashboardApi.instructorDashboard();
                Platform.runLater(() -> populate(resp));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load dashboard: " + ex.getMessage()));
            }
        });
    }

    /**
     * Populates summary labels and course table from the instructor dashboard response.
     */
    private void populate(InstructorDashboardResponse resp) {
        this.lastDashboard = resp;
        if (me != null) {
            welcomeLabel.setText("Welcome, " + (me.getFirstName() != null ? me.getFirstName() : me.getUsername()));
        }
        courseCountLabel.setText(String.valueOf(resp.getCourseCount()));
        enrollmentCountLabel.setText(String.valueOf(resp.getTotalEnrollments()));
        if (resp.getCourses() != null && !resp.getCourses().isEmpty()) {
            CourseDto last = resp.getCourses().get(resp.getCourses().size() - 1);
            lastCourseLabel.setText(last.getTitle());
        } else {
            lastCourseLabel.setText("No courses yet");
        }
        courses.setAll(resp.getCourses() != null ? resp.getCourses() : java.util.List.of());
        enrollments.clear();
        updateAnalyticsButton();
    }

    private void onOpenCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    private void onEditCourse(CourseDto course) {
        SessionStore.setActiveCourse(course);
        Launcher.go("course_editor.fxml", "Course Editor");
    }

    @FXML
    private void onViewEnrollments() {
        CourseDto selected = courseTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Select a course first."); return; }
        loadEnrollmentsForCourse(selected.getId());
    }

    /**
     * Loads enrollments for the selected course into the enrollments table.
     */
    private void loadEnrollmentsForCourse(Long courseId) {
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] resp = courseApi.listCourseEnrollments(courseId);
                Platform.runLater(() -> enrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of()));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load enrollments: " + ex.getMessage()));
            }
        });
    }

    /**
     * Loads courses the instructor is enrolled in (when student view is present).
     */
    private void loadMyEnrollments() {
        if (myEnrollmentTable == null || me == null || me.getId() == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] resp = enrollmentApi.listByCurrentUser(me.getId());
                Platform.runLater(() -> myEnrollments.setAll(resp != null ? java.util.Arrays.asList(resp) : java.util.List.of()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load your enrollments: " + ex.getMessage()));
            }
        });
    }

    /**
     * Builds a list of quizzes the instructor has not taken yet across enrolled courses.
     */
    private void loadUpcomingQuizzes() {
        if (upcomingQuizTable == null || me == null || me.getId() == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                EnrollmentResponse[] enrolls = enrollmentApi.listByCurrentUser(me.getId());
                if (enrolls == null) enrolls = new EnrollmentResponse[0];
                courseTitles.clear();
                List<QuizSummaryDto> pending = new ArrayList<>();
                Set<Long> added = new HashSet<>();
                for (EnrollmentResponse er : enrolls) {
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

    @FXML
    private void onCreateCourse() {
        SessionStore.setActiveCourse(null);
        Launcher.go("course_editor.fxml", "New Course");
    }

    private void openCourse(EnrollmentResponse er) {
        if (er == null || er.getCourseId() == null) return;
        CourseDto c = new CourseDto();
        c.setId(er.getCourseId());
        c.setTitle(er.getCourseTitle());
        SessionStore.setActiveCourse(c);
        Launcher.go("course_detail.fxml", "Course Detail");
    }

    @FXML
    /**
     * Opens the graphics playground when the user has data and permissions.
     */
    private void onOpenAnalytics() {
        if (!isInstructorOrAdmin()) return;
        if (lastDashboard == null || lastDashboard.getCourseCount() <= 0) {
            AlertUtils.warn("You need at least one course to view analytics.");
            return;
        }
        Launcher.go("graphics/graphics_playground.fxml", "3D Analytics");
    }

    private void onTakeQuiz(QuizSummaryDto quiz) {
        if (quiz == null) return;
        SessionStore.setActiveQuiz(quiz);
        CourseDto c = new CourseDto();
        c.setId(quiz.getCourseId());
        c.setTitle(courseTitles.getOrDefault(quiz.getCourseId(), ""));
        SessionStore.setActiveCourse(c);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    @FXML
    private void onOpenCatalog() { Launcher.go("course_catalog.fxml", "Course Catalog"); }

    @FXML
    private void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }

    /**
     * Shows/hides and enables/disables the analytics button based on role and data.
     */
    private void updateAnalyticsButton() {
        if (analyticsButton == null) return;
        boolean allowed = isInstructorOrAdmin() && me != null;
        boolean hasData = lastDashboard != null && lastDashboard.getCourseCount() > 0;
        analyticsButton.setVisible(allowed);
        analyticsButton.setManaged(allowed);
        analyticsButton.setDisable(!hasData);
    }

    private boolean isInstructorOrAdmin() {
        String role = SessionStore.currentRole();
        return "INSTRUCTOR".equals(role) || "ADMIN".equals(role);
    }
}
