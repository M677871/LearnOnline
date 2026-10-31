package com.csis231.api.quiz.controller;

import com.csis231.api.quiz.dto.QuizCreateRequest;
import com.csis231.api.quiz.dto.QuizDetailDto;
import com.csis231.api.quiz.dto.QuizQuestionRequest;
import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizSubmissionRequest;
import com.csis231.api.quiz.dto.QuizSubmissionResponse;
import com.csis231.api.quiz.dto.QuizSummaryDto;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.service.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/quizzes")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;
    private final AuthenticatedUserService authenticatedUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public QuizSummaryDto create(@Valid @RequestBody QuizCreateRequest request, Authentication authentication) {
        return quizService.createQuiz(request, authenticatedUserService.require(authentication));
    }

    @PostMapping("/{quizId}/questions")
    @ResponseStatus(HttpStatus.CREATED)
    public void addQuestions(@PathVariable Long quizId,
                             @Valid @RequestBody List<QuizQuestionRequest> questions,
                             Authentication authentication) {
        quizService.addQuestions(quizId, questions, authenticatedUserService.require(authentication));
    }

    @GetMapping("/{quizId}")
    public QuizDetailDto get(@PathVariable Long quizId, Authentication authentication) {
        return quizService.getQuizDetail(quizId, authenticatedUserService.require(authentication));
    }

    @PostMapping("/{quizId}/submit")
    public QuizSubmissionResponse submit(@PathVariable Long quizId,
                                         @Valid @RequestBody QuizSubmissionRequest request,
                                         Authentication authentication) {
        return quizService.submitQuiz(quizId, request, authenticatedUserService.require(authentication));
    }

    @GetMapping("/{quizId}/results")
    public List<QuizResultDto> results(@PathVariable Long quizId, Authentication authentication) {
        return quizService.resultsForQuiz(quizId, authenticatedUserService.require(authentication));
    }

    /**
     * Answers {@code 200} with an empty body when the user has no attempt yet.
     */
    @GetMapping("/{quizId}/my-result")
    public QuizResultDto myResult(@PathVariable Long quizId, Authentication authentication) {
        return quizService.latestResultForUser(quizId, authenticatedUserService.require(authentication));
    }

    @DeleteMapping("/{quizId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long quizId, Authentication authentication) {
        quizService.deleteQuiz(quizId, authenticatedUserService.require(authentication));
    }
}
