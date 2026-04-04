package com.csis231.api.quiz.controller;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.common.exception.UnauthorizedException;
import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.quiz.dto.*;
import com.csis231.api.quiz.mapper.QuizMapper;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.model.User;
import com.csis231.api.quiz.model.Quiz;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST endpoints for quiz authoring and delivery.
 */
@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {
    private final QuizService quizService;
    private final AuthenticatedUserService authenticatedUserService;

    /**
     * Creates a new quiz for the specified course.
     *
     * @param request        the quiz creation payload
     * @param authentication the authenticated instructor/admin creating the quiz
     * @return {@code 201 Created} with the created quiz summary
     */
    @PostMapping
    public ResponseEntity<QuizSummaryDto> create(@Valid @RequestBody QuizCreateRequest request,
                                                 Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        Quiz quiz = quizService.createQuiz(request, actor);
        QuizSummaryDto dto = QuizMapper.toSummaryDto(quiz, 0);
        return ResponseEntity.status(201).body(dto);
    }

    /**
     * Adds questions (and answers) to an existing quiz.
     *
     * @param quizId         the quiz identifier
     * @param questions      the questions to add
     * @param authentication the authenticated principal
     * @return {@code 201 Created} on success
     */
    @PostMapping("/{quizId}/questions")
    public ResponseEntity<Void> addQuestions(@PathVariable Long quizId,
                                             @Valid @RequestBody List<QuizQuestionRequest> questions,
                                             Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        quizService.addQuestions(quizId, questions, actor);
        return ResponseEntity.status(201).build();
    }

    /**
     * Retrieves quiz details (questions and options) for viewing/taking.
     *
     * @param quizId         the quiz identifier
     * @param authentication the authenticated principal
     * @return the {@link QuizDetailDto} for the quiz
     */
    @GetMapping("/{quizId}")
    public QuizDetailDto get(@PathVariable Long quizId, Authentication authentication) {
        User viewer = authenticatedUserService.require(authentication);
        return quizService.getQuizDetail(quizId, viewer);
    }

    /**
     * Submits quiz answers for grading.
     *
     * @param quizId         the quiz identifier
     * @param request        the submission payload
     * @param authentication the authenticated principal
     * @return a {@link QuizSubmissionResponse} summarizing the result
     */
    @PostMapping("/{quizId}/submit")
    public QuizSubmissionResponse submit(@PathVariable Long quizId,
                                         @Valid @RequestBody QuizSubmissionRequest request,
                                         Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        return quizService.submitQuiz(quizId, request, actor);
    }

    /**
     * Lists all results for a quiz (admin/instructor only).
     *
     * @param quizId         the quiz identifier
     * @param authentication the authenticated principal
     * @return list of {@link QuizResultDto} for the quiz
     */
    @GetMapping("/{quizId}/results")
    public List<QuizResultDto> results(@PathVariable Long quizId, Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        return quizService.resultsForQuiz(quizId, actor);
    }

    /**
     * Retrieves the most recent result for the current user on the given quiz.
     *
     * @param quizId         the quiz identifier
     * @param authentication the authenticated principal
     * @return the latest {@link QuizResultDto} for the user, or {@code null} if none
     */
    @GetMapping("/{quizId}/my-result")
    public QuizResultDto myResult(@PathVariable Long quizId, Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        return quizService.latestResultForUser(quizId, actor);
    }

    /**
     * Deletes a quiz and its related artifacts.
     *
     * @param quizId         the quiz identifier
     * @param authentication the authenticated principal
     * @return {@link ResponseEntity} with no content on success
     */
    @DeleteMapping("/{quizId}")
    public ResponseEntity<Void> delete(@PathVariable Long quizId, Authentication authentication) {
        User actor = authenticatedUserService.require(authentication);
        quizService.deleteQuiz(quizId, actor);
        return ResponseEntity.noContent().build();
    }
}
