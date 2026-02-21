package com.example.demo.course;

import com.example.demo.Launcher;
import com.example.demo.auth.AuthApi;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.common.TokenStore;
import com.example.demo.common.TableUtils;
import com.example.demo.model.*;
import com.example.demo.quiz.QuizApi;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.beans.property.SimpleStringProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

/**
 * Instructor course editor for creating/updating courses and managing materials/quizzes.
 *
 * <p>Loads the selected course (if any) from {@link SessionStore}, lets
 * instructors edit metadata, add/delete materials, create quizzes with
 * questions/answers, and review quiz results.</p>
 */
public class CourseEditorController {
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<com.example.demo.model.Category> categoryChoiceBox;
    @FXML private CheckBox publishedCheck;
    @FXML private Label courseIdLabel;
    @FXML private Button saveButton;

    // Materials
    @FXML private TableView<CourseMaterialDto> materialTable;
    @FXML private TableColumn<CourseMaterialDto, String> materialTitleColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialTypeColumn;
    @FXML private TableColumn<CourseMaterialDto, String> materialUrlColumn;
    @FXML private TextField materialTitleField;
    @FXML private TextField materialTypeField;
    @FXML private TextField materialUrlField;
    @FXML private TextField materialMetaField;

    // Quizzes
    @FXML private TableView<QuizSummaryDto> quizTable;
    @FXML private TableColumn<QuizSummaryDto, String> quizNameColumn;
    @FXML private TableColumn<QuizSummaryDto, Integer> quizQuestionsColumn;
    @FXML private TableColumn<QuizSummaryDto, Void> quizActionColumn;

    private final CourseApi courseApi = new CourseApi();
    private final QuizApi quizApi = new QuizApi();
    private final AuthApi authApi = new AuthApi();
    private final com.example.demo.admin.CategoryApi categoryApi = new com.example.demo.admin.CategoryApi();
    private final com.example.demo.admin.UserApi userApi = new com.example.demo.admin.UserApi();
    private CourseDto activeCourse;
    private CourseDetailDto activeDetail;
    private final ObservableList<CourseMaterialDto> materials = FXCollections.observableArrayList();
    private final ObservableList<QuizSummaryDto> quizzes = FXCollections.observableArrayList();

    /**
     * Configures tables, action buttons, and loads the current course and
     * category choices.
     */
    @FXML
    public void initialize() {
        materialTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        materialTypeColumn.setCellValueFactory(new PropertyValueFactory<>("materialType"));
        materialUrlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        materialTable.setItems(materials);

        quizNameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        quizQuestionsColumn.setCellValueFactory(new PropertyValueFactory<>("questionCount"));
        quizActionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button viewBtn = new Button("Results");
            private final Button deleteBtn = new Button("Delete");
            {
                viewBtn.getStyleClass().add("ghost-button");
                deleteBtn.getStyleClass().add("danger-button");
                viewBtn.setOnAction(e -> {
                    QuizSummaryDto q = getTableView().getItems().get(getIndex());
                    onViewResults(q);
                });
                deleteBtn.setOnAction(e -> {
                    QuizSummaryDto q = getTableView().getItems().get(getIndex());
                    onDeleteQuiz(q);
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                if (empty || getIndex() >= getTableView().getItems().size()) {
                    setGraphic(null);
                    return;
                }
                HBox box = new HBox(8, viewBtn, deleteBtn);
                setGraphic(box);
            }
        });
        quizTable.setItems(quizzes);
        TableUtils.style(materialTable, materialTitleColumn, materialTypeColumn, materialUrlColumn);
        TableUtils.style(quizTable, quizNameColumn, quizQuestionsColumn, quizActionColumn);

        loadCourse();
        loadCategories();
    }

    /**
     * Loads the active course (if set in {@link SessionStore}) or prepares a
     * blank editor for a new course.
     */
    private void loadCourse() {
        CompletableFuture.runAsync(() -> {
            try {
                CourseDto selected = SessionStore.getActiveCourse();
                if (selected != null && selected.getId() != null) {
                    CourseDetailDto detail = courseApi.get(selected.getId());
                    activeCourse = selected;
                    activeDetail = detail;
                    SessionStore.setActiveCourse(selected);
                    Platform.runLater(() -> populate(detail));
                } else {
                    Platform.runLater(this::populateEmpty);
                }
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load course: " + ex.getMessage()));
            }
        });
    }

    /**
     * Clears the form for creating a new course.
     */
    private void populateEmpty() {
        courseIdLabel.setText("New course");
        titleField.clear();
        descriptionArea.clear();
        categoryChoiceBox.getSelectionModel().clearSelection();
        publishedCheck.setSelected(true);
        materials.clear();
        quizzes.clear();
    }

    /**
     * Populates the editor with an existing course's details.
     */
    private void populate(CourseDetailDto detail) {
        if (detail == null) { populateEmpty(); return; }
        courseIdLabel.setText("Course #" + detail.getId());
        titleField.setText(detail.getTitle());
        descriptionArea.setText(detail.getDescription());
        if (detail.getCategoryId() != null) {
            categoryChoiceBox.getItems().stream()
                    .filter(c -> detail.getCategoryId().equals(c.getId()))
                    .findFirst()
                    .ifPresent(c -> categoryChoiceBox.getSelectionModel().select(c));
        } else {
            categoryChoiceBox.getSelectionModel().clearSelection();
        }
        publishedCheck.setSelected(Boolean.TRUE.equals(detail.getPublished()));
        materials.setAll(detail.getMaterials() != null ? detail.getMaterials() : List.of());
        quizzes.setAll(detail.getQuizzes() != null ? detail.getQuizzes() : List.of());
    }

    /**
     * Creates or updates a course based on the current form values.
     */
    @FXML
    private void onSaveCourse() {
        String title = trim(titleField.getText());
        if (title.isEmpty()) { AlertUtils.warn("Title is required."); return; }
        Long categoryId = null;
        var selectedCategory = categoryChoiceBox.getSelectionModel().getSelectedItem();
        if (selectedCategory != null) {
            categoryId = selectedCategory.getId();
        }
        CourseRequest req = new CourseRequest(
                title,
                trim(descriptionArea.getText()),
                categoryId,
                publishedCheck.isSelected()
        );

        saveButton.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                CourseDto updated;
                if (activeCourse != null && activeCourse.getId() != null) {
                    updated = courseApi.update(activeCourse.getId(), req);
                } else {
                    updated = courseApi.create(req);
                }
                SessionStore.setActiveCourse(updated);
                loadCourse();
                Platform.runLater(() -> AlertUtils.info("Course saved"));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to save course: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> saveButton.setDisable(false));
            }
        });
    }

    /**
     * Deletes the active course and returns to the instructor dashboard.
     */
    @FXML
    private void onDeleteCourse() {
        if (activeCourse == null || activeCourse.getId() == null) {
            AlertUtils.warn("Select or save a course before deleting."); return;
        }
        CompletableFuture.runAsync(() -> {
            try {
                courseApi.delete(activeCourse.getId());
                SessionStore.setActiveCourse(null);
                Platform.runLater(() -> {
                    AlertUtils.info("Course deleted");
                    Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to delete course: " + ex.getMessage()));
            }
        });
    }

    /**
     * Adds a course material using the form inputs.
     */
    @FXML
    private void onAddMaterial() {
        if (!ensureCourseExists()) return;
        String title = trim(materialTitleField.getText());
        if (title.isEmpty()) { AlertUtils.warn("Material title required."); return; }
        CourseMaterialRequest req = new CourseMaterialRequest(
                title,
                trim(materialTypeField.getText()),
                trim(materialUrlField.getText()),
                trim(materialMetaField.getText())
        );
        CompletableFuture.runAsync(() -> {
            try {
                CourseMaterialDto created = courseApi.addMaterial(activeCourse.getId(), req);
                Platform.runLater(() -> {
                    materials.add(created);
                    materialTitleField.clear();
                    materialTypeField.clear();
                    materialUrlField.clear();
                    materialMetaField.clear();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to add material: " + ex.getMessage()));
            }
        });
    }

    /**
     * Deletes the selected material from the course.
     */
    @FXML
    private void onDeleteMaterial() {
        CourseMaterialDto selected = materialTable.getSelectionModel().getSelectedItem();
        if (selected == null) { AlertUtils.warn("Select a material to delete."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                courseApi.deleteMaterial(selected.getId());
                Platform.runLater(() -> materials.remove(selected));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to delete material: " + ex.getMessage()));
            }
        });
    }

    /**
     * Creates a new quiz with questions by walking the user through dialogs.
     */
    @FXML
    private void onCreateQuiz() {
        if (!ensureCourseExists()) return;
        promptQuizCreation().ifPresent(payload -> {
            CompletableFuture.runAsync(() -> {
                try {
                    QuizSummaryDto quiz = quizApi.createQuiz(
                            new QuizCreateRequest(activeCourse.getId(), payload.name, payload.description));
                    quizApi.addQuestions(quiz.getId(), payload.questions);
                    loadCourse();
                    Platform.runLater(() -> AlertUtils.info("Quiz created with " + payload.questions.size() + " question(s)"));
                } catch (ApiException ex) {
                    Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
                } catch (Exception ex) {
                    Platform.runLater(() -> ErrorDialog.showError("Failed to create quiz: " + ex.getMessage()));
                }
            });
        });
    }

    /**
     * Shows dialogs to collect quiz metadata and one or more questions.
     *
     * @return built quiz payload or empty if cancelled/invalid
     */
    private Optional<QuizPayload> promptQuizCreation() {
        Dialog<QuizMeta> metaDialog = new Dialog<>();
        metaDialog.setTitle("Create Quiz");
        metaDialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField nameField = new TextField();
        nameField.setPromptText("Quiz title");
        TextField descField = new TextField();
        descField.setPromptText("Description");

        GridPane metaGrid = new GridPane();
        metaGrid.setVgap(8); metaGrid.setHgap(8);
        metaGrid.addRow(0, new Label("Name"), nameField);
        metaGrid.addRow(1, new Label("Description"), descField);
        metaDialog.getDialogPane().setContent(metaGrid);

        metaDialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (nameField.getText().isBlank()) {
                AlertUtils.warn("Enter quiz name.");
                return null;
            }
            return new QuizMeta(nameField.getText().trim(), descField.getText().trim());
        });

        Optional<QuizMeta> metaOpt = metaDialog.showAndWait();
        if (metaOpt.isEmpty()) return Optional.empty();

        List<QuizQuestionRequest> questions = new ArrayList<>();
        boolean keepAdding = true;
        while (keepAdding) {
            Dialog<QuizQuestionRequest> qDialog = buildQuestionDialog(questions.isEmpty());
            Optional<QuizQuestionRequest> q = qDialog.showAndWait();
            if (q.isPresent()) {
                questions.add(q.get());
            } else {
                keepAdding = false;
            }
        }

        if (questions.isEmpty()) {
            AlertUtils.warn("A quiz must contain at least one question.");
            return Optional.empty();
        }

        QuizMeta meta = metaOpt.get();
        return Optional.of(new QuizPayload(meta.name, meta.description, questions));
    }

    /**
     * Builds a dialog for a single quiz question and its options.
     *
     * @param first whether this is the first question (affects dialog title)
     */
    private Dialog<QuizQuestionRequest> buildQuestionDialog(boolean first) {
        Dialog<QuizQuestionRequest> dialog = new Dialog<>();
        dialog.setTitle(first ? "Add first question" : "Add another question");
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField questionField = new TextField();
        questionField.setPromptText("Question text");
        TextField optA = new TextField();
        optA.setPromptText("Option A");
        TextField optB = new TextField();
        optB.setPromptText("Option B");
        TextField optC = new TextField();
        optC.setPromptText("Option C (optional)");
        TextField optD = new TextField();
        optD.setPromptText("Option D (optional)");

        ComboBox<String> correctBox = new ComboBox<>();
        correctBox.getItems().addAll("A","B","C","D");
        correctBox.getSelectionModel().selectFirst();

        GridPane grid = new GridPane();
        grid.setVgap(8); grid.setHgap(8);
        grid.addRow(0, new Label("Question"), questionField);
        grid.addRow(1, new Label("Option A"), optA);
        grid.addRow(2, new Label("Option B"), optB);
        grid.addRow(3, new Label("Option C"), optC);
        grid.addRow(4, new Label("Option D"), optD);
        grid.addRow(5, new Label("Correct"), correctBox);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(btn -> {
            if (btn != ButtonType.OK) return null;
            if (questionField.getText().isBlank() || optA.getText().isBlank() || optB.getText().isBlank()) {
                AlertUtils.warn("Enter question and at least two options.");
                return null;
            }
            List<AnswerCreateRequest> answers = new ArrayList<>();
            answers.add(new AnswerCreateRequest(optA.getText().trim(), "A".equals(correctBox.getValue())));
            answers.add(new AnswerCreateRequest(optB.getText().trim(), "B".equals(correctBox.getValue())));
            if (!optC.getText().isBlank()) {
                answers.add(new AnswerCreateRequest(optC.getText().trim(), "C".equals(correctBox.getValue())));
            }
            if (!optD.getText().isBlank()) {
                answers.add(new AnswerCreateRequest(optD.getText().trim(), "D".equals(correctBox.getValue())));
            }
            boolean hasCorrect = answers.stream().anyMatch(AnswerCreateRequest::isCorrect);
            if (!hasCorrect) {
                answers.get(0).setCorrect(true);
            }
            return new QuizQuestionRequest(questionField.getText().trim(), answers);
        });
        return dialog;
    }

    /**
     * Loads categories for the course selector.
     */
    private void loadCategories() {
        CompletableFuture.runAsync(() -> {
            try {
                var page = categoryApi.list(0, 100);
                var items = page != null && page.getContent() != null ? page.getContent() : List.<com.example.demo.model.Category>of();
                Platform.runLater(() -> {
                    categoryChoiceBox.getItems().setAll(items);
                    categoryChoiceBox.setCellFactory(listView -> new ListCell<>() {
                        @Override protected void updateItem(com.example.demo.model.Category item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item.getName() + " (ID " + item.getId() + ")");
                        }
                    });
                    categoryChoiceBox.setButtonCell(new ListCell<>() {
                        @Override protected void updateItem(com.example.demo.model.Category item, boolean empty) {
                            super.updateItem(item, empty);
                            setText(empty || item == null ? null : item.getName());
                        }
                    });
                });
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load categories: " + ex.getMessage()));
            }
        });
    }

    /**
     * Retrieves quiz results and shows them in a dialog.
     */
    private void onViewResults(QuizSummaryDto quiz) {
        if (quiz == null) { AlertUtils.warn("Select a quiz first."); return; }
        CompletableFuture.runAsync(() -> {
            try {
                QuizResultDto[] res = quizApi.results(quiz.getId());
                Map<Long, String> names = new HashMap<>();
                if (res != null) {
                    for (QuizResultDto r : res) {
                        if (r != null && r.getStudentUserId() != null && !names.containsKey(r.getStudentUserId())) {
                            try {
                                var u = userApi.get(r.getStudentUserId());
                                if (u != null && u.getUsername() != null) {
                                    names.put(r.getStudentUserId(), u.getUsername());
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                }
                Platform.runLater(() -> showResultsDialog(quiz, res != null ? List.of(res) : List.of(), names));
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load results: " + ex.getMessage()));
            }
        });
    }

    /**
     * Displays quiz results in a modal table with usernames where available.
     */
    private void showResultsDialog(QuizSummaryDto quiz, List<QuizResultDto> results, Map<Long, String> usernames) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Results - " + (quiz != null ? quiz.getName() : ""));

        TableView<QuizResultDto> table = new TableView<>();

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setItems(FXCollections.observableArrayList(results));

        TableColumn<QuizResultDto, String> studentCol = new TableColumn<>("Student");
        studentCol.setCellValueFactory(c -> {
            QuizResultDto r = c.getValue();
            Long id = r.getStudentUserId();
            String name = id != null ? usernames.getOrDefault(id, String.valueOf(id)) : "";
            return new SimpleStringProperty(name);
        });
        TableColumn<QuizResultDto, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(c -> {
            QuizResultDto r = c.getValue();
            return new SimpleStringProperty(r.getScore() + "/" + r.getTotalQuestions());
        });
        TableColumn<QuizResultDto, String> completedCol = new TableColumn<>("Completed At");
        completedCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getCompletedAt() != null ? c.getValue().getCompletedAt().toString() : ""
        ));

        table.getColumns().addAll(studentCol, scoreCol, completedCol);
        TableUtils.style(table, studentCol, scoreCol, completedCol);
        table.setPrefHeight(360);

        VBox root = new VBox(12, new Label("Results"), table);
        root.setPadding(new javafx.geometry.Insets(16));
        VBox.setVgrow(table, Priority.ALWAYS);

        Scene scene = new Scene(root, 600, 420);
        String css = com.example.demo.HelloApplication.class
                .getResource("/com/example/demo/styles.css")
                .toExternalForm();
        scene.getStylesheets().add(css);
        stage.setScene(scene);
        stage.showAndWait();
    }

    /**
     * Deletes a quiz and removes it from the table.
     */
    private void onDeleteQuiz(QuizSummaryDto quiz) {
        if (quiz == null) return;
        CompletableFuture.runAsync(() -> {
            try {
                quizApi.deleteQuiz(quiz.getId());
                Platform.runLater(() -> {
                    quizzes.remove(quiz);
                    AlertUtils.info("Quiz deleted");
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to delete quiz: " + ex.getMessage()));
            }
        });
    }

    /**
     * Ensures a course exists before performing child operations.
     */
    private boolean ensureCourseExists() {
        if (activeCourse != null && activeCourse.getId() != null) return true;
        AlertUtils.warn("Save the course before adding materials or quizzes.");
        return false;
    }

    private static String trim(String v) { return v == null ? "" : v.trim(); }

    /**
     * Navigates back to a role-appropriate dashboard.
     */
    @FXML
    private void onBack() {
        var me = SessionStore.getMe();
        if (me != null && "ADMIN".equalsIgnoreCase(me.getRole())) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else if (me != null && "INSTRUCTOR".equalsIgnoreCase(me.getRole())) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }

    /**
     * Clears auth/session state and returns to login.
     */
    @FXML
    private void onLogout() {
        TokenStore.clear();
        SessionStore.clearAll();
        Launcher.go("login.fxml", "Login");
    }

    private record QuizMeta(String name, String description) {}
    private record QuizPayload(String name, String description, List<QuizQuestionRequest> questions) {}
}
