package com.example.demo.quiz;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

/**
 * HTTP wrapper for quiz endpoints (create, fetch, submit, results).
 */
public class QuizApi {
    private final ApiClient client = new ApiClient();

    /**
     * Fetches a full quiz with questions/options.
     */
    public QuizDetailDto getQuiz(Long quizId) {
        ApiResponse<QuizDetailDto> resp = client.get("/api/quizzes/" + quizId, new TypeReference<QuizDetailDto>() {});
        return resp.getBody();
    }

    /**
     * Creates a quiz and returns its summary.
     */
    public QuizSummaryDto createQuiz(QuizCreateRequest req) {
        ApiResponse<QuizSummaryDto> resp = client.post("/api/quizzes", req, new TypeReference<QuizSummaryDto>() {});
        return resp.getBody();
    }

    /**
     * Adds a batch of questions to a quiz.
     */
    public void addQuestions(Long quizId, List<QuizQuestionRequest> questions) {
        client.post("/api/quizzes/" + quizId + "/questions", questions);
    }

    /**
     * Submits quiz answers and returns the scored result.
     */
    public QuizSubmissionResponse submit(Long quizId, QuizSubmissionRequest req) {
        ApiResponse<QuizSubmissionResponse> resp = client.post("/api/quizzes/" + quizId + "/submit", req, new TypeReference<QuizSubmissionResponse>() {});
        return resp.getBody();
    }

    /**
     * Lists all results for a quiz (for instructors/admins).
     */
    public QuizResultDto[] results(Long quizId) {
        ApiResponse<QuizResultDto[]> resp = client.get("/api/quizzes/" + quizId + "/results", new TypeReference<QuizResultDto[]>() {});
        return resp.getBody();
    }

    /**
     * Deletes a quiz by id.
     */
    public void deleteQuiz(Long quizId) {
        client.delete("/api/quizzes/" + quizId);
    }

    /**
     * Fetches the current user's latest result for a quiz, if any.
     */
    public QuizResultDto myResult(Long quizId) {
        ApiResponse<QuizResultDto> resp = client.get("/api/quizzes/" + quizId + "/my-result", new TypeReference<QuizResultDto>() {});
        return resp.getBody();
    }
}
