package com.example.demo.course;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.CourseDetailDto;
import com.example.demo.model.CourseDto;
import com.example.demo.model.CourseMaterialDto;
import com.example.demo.model.QuizSummaryDto;
import com.example.demo.model.QuizResultDto;
import com.example.demo.quiz.QuizApi;
import javafx.beans.property.SimpleStringProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.cell.PropertyValueFactory;


import java.awt.*;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Course detail view for students and instructors.
 *
 * <p>Displays course metadata, materials, and quizzes, allows opening links or
 * files for materials, and lets the user start quizzes while showing their
 * latest scores.</p>
 */
public class CourseDetailController {
    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label instructorLabel;
    @FXML private Label publishedLabel;

    @FXML private TableView<CourseMaterialDto> materialTable;
    @FXML private TableColumn<CourseMaterialDto, String> materialTitleColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialTypeColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialUrlColumn;

    @FXML private TableView<QuizSummaryDto> quizTable;
    @FXML private TableColumn<QuizSummaryDto, String> quizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> quizQuestionsColumn;
    @FXML private TableColumn<QuizSummaryDto, String> quizResultColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> quizActionColumn;

    private final CourseApi courseApi = new CourseApi();
    private final QuizApi quizApi = new QuizApi();
    private final ObservableList<CourseMaterialDto> materials = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> quizzes = FXCollections.observableArrayList();
    private final Map<Long, QuizResultDto> latestResults = new ConcurrentHashMap<>();
    private CourseDto course;

    /**
     * Sets up tables, loads the active course from {@link SessionStore}, and
     * fetches detail plus quiz results.
     */
    @FXML
    public void initialize() {
        materialTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        materialTypeColumn.setCellValueFactory(new PropertyValueFactory<>("materialType"));
        materialUrlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        materialTable.setItems(materials);
        materialTable.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) openSelectedMaterial();
        });

        quizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quizQuestionsColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
        quizResultColumn.setCellValueFactory(c -> new SimpleStringProperty(""));
        quizResultColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setText(null);
                    return;
                }
                QuizSummaryDto q = getTableView().getItems().get(getIndex());
                QuizResultDto r = latestResults.get(q.getId());
                if (r == null) {
                    setText("—");
                } else {
                    setText(r.getScore() + "/" + r.getTotalQuestions());
                }
            }
        });

        quizActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button takeBtn = new Button("Take");
            {
                takeBtn.getStyleClass().add("primary-button");
                takeBtn.setOnAction(e -> {
                    QuizSummaryDto q = getTableView().getItems().get(getIndex());
                    onTakeQuiz(q);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                QuizSummaryDto q = getTableView().getItems().get(getIndex());
                boolean hasResult = latestResults.containsKey(q.getId());
                takeBtn.setText(hasResult ? "Retry" : "Take");
                setGraphic(takeBtn);
            }
        });
        quizTable.setItems(quizzes);
        TableUtils.style(materialTable, materialTitleColumn, materialTypeColumn, materialUrlColumn);
        TableUtils.style(quizTable, quizNameColumn, quizQuestionsColumn, quizResultColumn, quizActionColumn);

        course = SessionStore.getActiveCourse();
        if (course == null) {
            AlertUtils.warn("No course selected.");
            return;
        }
        loadDetail(course.getId());
    }

    /**
     * Loads course detail asynchronously and populates the view.
     */
    private void loadDetail(Long courseId) {
        CompletableFuture.runAsync(() -> {
            try {
                CourseDetailDto detail = courseApi.get(courseId);
                Platform.runLater(() -> populate(detail));
                loadLatestResults(detail);
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load course: " + ex.getMessage()));
            }
        });
    }

    /**
     * Fetches the viewer's latest results for each quiz and refreshes the table
     * as results arrive.
     */
    private void loadLatestResults(CourseDetailDto d) {
        if (d == null || d.getQuizzes() == null) return;
        for (QuizSummaryDto quiz : d.getQuizzes()) {
            CompletableFuture.runAsync(() -> {
                try {
                    QuizResultDto res = quizApi.myResult(quiz.getId());
                    if (res != null) {
                        latestResults.put(quiz.getId(), res);
                        Platform.runLater(() -> quizTable.refresh());
                    }
                } catch (Exception ignored) {}
            });
        }
    }

    /**
     * Populates labels and tables with course information.
     */
    private void populate(CourseDetailDto d) {
        titleLabel.setText(d.getTitle());
        descriptionLabel.setText(d.getDescription());
        instructorLabel.setText(d.getInstructorName());
        publishedLabel.setText(Boolean.TRUE.equals(d.getPublished()) ? "Published" : "Draft");
        materials.setAll(d.getMaterials() != null ? d.getMaterials() : java.util.List.of());
        quizzes.setAll(d.getQuizzes() != null ? d.getQuizzes() : java.util.List.of());
    }

    /**
     * Opens the selected course material in the default browser or associated
     * application, handling both URLs and local file paths.
     */
    private void openSelectedMaterial() {
        CourseMaterialDto selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        String raw = selected.getUrl();
        if (raw == null || raw.isBlank()) {
            return;
        }

        if (!Desktop.isDesktopSupported()) {
            AlertUtils.warn("Opening materials is not supported on this system.");
            return;
        }

        String url = raw.trim();

        try {
            // If it looks like a bare domain, prefix https://
            if (!url.matches("(?i)^https?://.*")
                    && url.matches("(?i)^[a-z0-9][a-z0-9.-]*\\.[a-z]{2,}.*")) {
                url = "https://" + url;
            }

            boolean isHttp = url.matches("(?i)^https?://.*");

            if (isHttp) {
                // ✅ Web link: open in browser, no Path.of(...) here
                String safe = url.replace("<", "")
                        .replace(">", "")
                        .replace(" ", "%20");
                Desktop.getDesktop().browse(new URI(safe));

            } else {
                // ✅ Local file path: use Path.of only here
                Path file = Path.of(url);
                if (Files.exists(file)) {
                    Desktop.getDesktop().open(file.toFile());
                } else {
                    AlertUtils.warn("File does not exist: " + url);
                }
            }

        } catch (Exception ex) {
            ErrorDialog.showError("Could not open material: " + ex.getMessage());
        }
    }


    /**
     * Starts or retries the selected quiz.
     */
    private void onTakeQuiz(QuizSummaryDto quiz) {
        if (quiz == null) { return; }
        SessionStore.setActiveQuiz(quiz);
        Launcher.go("quiz_taker.fxml", "Take Quiz");
    }

    /**
     * Returns to the dashboard that matches the current user's role.
     */
    @FXML
    private void onBack() {
        var me = SessionStore.getMe();
        if (me != null && "INSTRUCTOR".equalsIgnoreCase(me.getRole())) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else if (me != null && "ADMIN".equalsIgnoreCase(me.getRole())) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }
}
