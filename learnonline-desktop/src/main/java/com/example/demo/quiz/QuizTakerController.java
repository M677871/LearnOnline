package com.example.demo.quiz;

import com.example.demo.Launcher;
import com.example.demo.common.AlertUtils;
import com.example.demo.common.ApiException;
import com.example.demo.common.ErrorDialog;
import com.example.demo.common.SessionStore;
import com.example.demo.model.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Simple quiz-taking screen that fetches a quiz and walks the user through
 * one question at a time, collecting answers and submitting for scoring.
 */
public class QuizTakerController {
    @FXML private Label quizTitleLabel;
    @FXML private Label progressLabel;
    @FXML private Label questionLabel;
    @FXML private VBox answersBox;
    @FXML private Button nextButton;

    private final QuizApi quizApi = new QuizApi();
    private QuizDetailDto quiz;
    private int index = 0;
    private final Map<Long, Long> answers = new HashMap<>();

    /**
     * Loads the selected quiz from {@link SessionStore} and fetches details.
     */
    @FXML
    public void initialize() {
        QuizSummaryDto selected = SessionStore.getActiveQuiz();
        if (selected == null) {
            AlertUtils.warn("No quiz selected.");
            return;
        }
        quizTitleLabel.setText(selected.getName());
        loadQuiz(selected.getId());
    }

    /**
     * Fetches quiz detail and renders the first question.
     */
    private void loadQuiz(Long quizId) {
        CompletableFuture.runAsync(() -> {
            try {
                QuizDetailDto detail = quizApi.getQuiz(quizId);
                Platform.runLater(() -> {
                    quiz = detail;
                    showCurrent();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to load quiz: " + ex.getMessage()));
            }
        });
    }

    /**
     * Shows the current question and previously selected answer (if any).
     */
    private void showCurrent() {
        if (quiz == null || quiz.getQuestions() == null || quiz.getQuestions().isEmpty()) {
            questionLabel.setText("No questions in this quiz yet.");
            nextButton.setDisable(true);
            return;
        }
        index = Math.max(0, Math.min(index, quiz.getQuestions().size() - 1));
        QuizQuestionDto q = quiz.getQuestions().get(index);
        questionLabel.setText(q.getQuestionText());
        progressLabel.setText("Question " + (index + 1) + " of " + quiz.getQuestions().size());

        ToggleGroup group = new ToggleGroup();
        answersBox.getChildren().clear();
        for (AnswerOptionDto opt : q.getOptions()) {
            RadioButton rb = new RadioButton(opt.getAnswerText());
            rb.setToggleGroup(group);
            rb.setUserData(opt.getId());
            if (answers.get(q.getId()) != null && answers.get(q.getId()).equals(opt.getId())) {
                rb.setSelected(true);
            }
            answersBox.getChildren().add(rb);
        }

        nextButton.setText(index == quiz.getQuestions().size() - 1 ? "Submit" : "Next");
    }

    /**
     * Advances to the next question or submits on the last question.
     */
    @FXML
    private void onNext() {
        if (quiz == null) return;
        QuizQuestionDto q = quiz.getQuestions().get(index);
        Long selected = getSelectedAnswer();
        if (selected != null) {
            answers.put(q.getId(), selected);
        }

        if (index == quiz.getQuestions().size() - 1) {
            submit();
        } else {
            index++;
            showCurrent();
        }
    }

    /**
     * Reads the selected radio button answer for the current question.
     */
    private Long getSelectedAnswer() {
        for (var node : answersBox.getChildren()) {
            if (node instanceof RadioButton rb && rb.isSelected()) {
                Object data = rb.getUserData();
                return data instanceof Long l ? l : Long.valueOf(data.toString());
            }
        }
        return null;
    }

    /**
     * Builds the submission payload and posts it for scoring.
     */
    private void submit() {
        if (quiz == null) return;
        var payload = quiz.getQuestions().stream()
                .filter(q -> answers.containsKey(q.getId()))
                .map(q -> new QuizSubmissionAnswer(q.getId(), answers.get(q.getId())))
                .toList();
        if (payload.isEmpty()) {
            AlertUtils.warn("Please answer at least one question.");
            return;
        }
        nextButton.setDisable(true);
        CompletableFuture.runAsync(() -> {
            try {
                QuizSubmissionResponse resp = quizApi.submit(quiz.getId(), new QuizSubmissionRequest(payload));
                Platform.runLater(() -> {
                    AlertUtils.info("Score: " + resp.getScore() + "/" + resp.getTotalQuestions()
                            + " (" + Math.round(resp.getPercentage()) + "%)");
                    navigateHome();
                });
            } catch (ApiException ex) {
                Platform.runLater(() -> ErrorDialog.showError(ex.getMessage(), ex.getErrorCode()));
            } catch (Exception ex) {
                Platform.runLater(() -> ErrorDialog.showError("Failed to submit quiz: " + ex.getMessage()));
            } finally {
                Platform.runLater(() -> nextButton.setDisable(false));
            }
        });
    }

    /**
     * Navigates back to the role-appropriate dashboard.
     */
    @FXML
    private void onBack() { navigateHome(); }

    private void navigateHome() {
        String role = SessionStore.currentRole();
        if ("ADMIN".equalsIgnoreCase(role)) {
            Launcher.go("dashboard.fxml", "Admin Dashboard");
        } else if ("INSTRUCTOR".equalsIgnoreCase(role)) {
            Launcher.go("instructor_dashboard.fxml", "Instructor Dashboard");
        } else {
            Launcher.go("student_dashboard.fxml", "Student Dashboard");
        }
    }
}
