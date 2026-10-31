package com.csis231.api.quiz.service;

import com.csis231.api.common.exception.BadRequestException;
import com.csis231.api.common.exception.ForbiddenException;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.repository.CourseRepository;
import com.csis231.api.course.service.CourseAccess;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.quiz.dto.AnswerCreateRequest;
import com.csis231.api.quiz.dto.QuizCreateRequest;
import com.csis231.api.quiz.dto.QuizDetailDto;
import com.csis231.api.quiz.dto.QuizQuestionRequest;
import com.csis231.api.quiz.dto.QuizResultDto;
import com.csis231.api.quiz.dto.QuizSubmissionAnswer;
import com.csis231.api.quiz.dto.QuizSubmissionRequest;
import com.csis231.api.quiz.dto.QuizSubmissionResponse;
import com.csis231.api.quiz.dto.QuizSummaryDto;
import com.csis231.api.quiz.mapper.QuizMapper;
import com.csis231.api.quiz.model.AnswerOption;
import com.csis231.api.quiz.model.Quiz;
import com.csis231.api.quiz.model.QuizQuestion;
import com.csis231.api.quiz.model.QuizResult;
import com.csis231.api.quiz.repository.AnswerOptionRepository;
import com.csis231.api.quiz.repository.QuizQuestionRepository;
import com.csis231.api.quiz.repository.QuizRepository;
import com.csis231.api.quiz.repository.QuizResultRepository;
import com.csis231.api.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quiz authoring, delivery and grading.
 */
@Service
@RequiredArgsConstructor
public class QuizService {

    private static final String NOT_STAFF = "Only instructors or admins can manage quizzes";
    private static final String NOT_OWNER = "You can only manage quizzes for your own courses";

    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizResultRepository resultRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    @Transactional
    public QuizSummaryDto createQuiz(QuizCreateRequest req, User actor) {
        CourseAccess.requireStaff(actor, NOT_STAFF);
        Course course = courseRepository.getOrThrow(req.courseId());
        requireManager(course, actor);

        Quiz quiz = quizRepository.save(Quiz.builder()
                .course(course)
                .name(req.name())
                .description(req.description())
                .build());
        return QuizMapper.toSummaryDto(quiz, 0);
    }

    @Transactional
    public void addQuestions(Long quizId, List<QuizQuestionRequest> requests, User actor) {
        if (requests.isEmpty()) {
            throw new BadRequestException("Questions are required");
        }
        Quiz quiz = quizRepository.getOrThrow(quizId);
        requireManager(quiz.getCourse(), actor);

        for (QuizQuestionRequest request : requests) {
            if (request.answers().stream().noneMatch(AnswerCreateRequest::correct)) {
                throw new BadRequestException("Each question must have at least one correct answer");
            }
            QuizQuestion question = questionRepository.save(QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(request.questionText())
                    .build());
            answerOptionRepository.saveAll(request.answers().stream()
                    .map(answer -> AnswerOption.builder()
                            .question(question)
                            .answerText(answer.answerText())
                            .correct(answer.correct())
                            .build())
                    .toList());
        }
    }

    /**
     * Returns the questions and options of a quiz, without revealing which options are correct.
     */
    @Transactional(readOnly = true)
    public QuizDetailDto getQuizDetail(Long quizId, User viewer) {
        Quiz quiz = quizRepository.getOrThrow(quizId);
        requireCanView(quiz, viewer);

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        return QuizMapper.toDetailDto(quiz, questions, loadOptionsByQuestion(questions));
    }

    /**
     * Grades a submission and stores the attempt. Questions left unanswered count as wrong.
     */
    @Transactional
    public QuizSubmissionResponse submitQuiz(Long quizId, QuizSubmissionRequest request, User actor) {
        Quiz quiz = quizRepository.getOrThrow(quizId);
        requireCanSubmit(quiz, actor);

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        int score = grade(questions, request.answers());
        int totalQuestions = questions.size();

        QuizResult result = resultRepository.save(QuizResult.builder()
                .quiz(quiz)
                .student(actor)
                .score(score)
                .totalQuestions(totalQuestions)
                .build());

        double percentage = totalQuestions == 0 ? 0 : score * 100.0 / totalQuestions;
        return new QuizSubmissionResponse(
                quiz.getId(),
                actor.getId(),
                score,
                totalQuestions,
                percentage,
                result.getCompletedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<QuizResultDto> resultsForQuiz(Long quizId, User actor) {
        requireManager(quizRepository.getOrThrow(quizId).getCourse(), actor);
        return resultRepository.findByQuiz_Id(quizId).stream()
                .map(QuizMapper::toResultDto)
                .toList();
    }

    /**
     * Returns the actor's latest attempt, or {@code null} if there is none.
     */
    @Transactional(readOnly = true)
    public QuizResultDto latestResultForUser(Long quizId, User actor) {
        requireCanView(quizRepository.getOrThrow(quizId), actor);
        return resultRepository.findTop1ByQuiz_IdAndStudent_IdOrderByCompletedAtDesc(quizId, actor.getId())
                .map(QuizMapper::toResultDto)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<QuizResultDto> latestResultsForStudent(Long studentId) {
        return resultRepository.findTop5ByStudent_IdOrderByCompletedAtDesc(studentId).stream()
                .map(QuizMapper::toResultDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<QuizSummaryDto> summariesForCourse(Long courseId) {
        return quizRepository.findByCourse_Id(courseId).stream()
                .map(quiz -> QuizMapper.toSummaryDto(quiz, questionRepository.countByQuiz_Id(quiz.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public int countForCourse(Long courseId) {
        return quizRepository.countByCourse_Id(courseId);
    }

    /**
     * Deletes a quiz with its dependent rows, children first because the foreign keys have no cascade.
     */
    @Transactional
    public void deleteQuiz(Long quizId, User actor) {
        Quiz quiz = quizRepository.getOrThrow(quizId);
        requireManager(quiz.getCourse(), actor);

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        answerOptionRepository.deleteAll(
                answerOptionRepository.findByQuestion_IdIn(questions.stream().map(QuizQuestion::getId).toList()));
        questionRepository.deleteAll(questions);
        resultRepository.deleteAll(resultRepository.findByQuiz_Id(quizId));
        quizRepository.delete(quiz);
    }

    /**
     * Counts correct answers. Each question may be answered only once, otherwise repeating a correct
     * answer would push the score above the number of questions.
     */
    private int grade(List<QuizQuestion> questions, List<QuizSubmissionAnswer> answers) {
        Map<Long, List<AnswerOption>> optionsByQuestion = loadOptionsByQuestion(questions);
        Set<Long> quizQuestionIds = questions.stream().map(QuizQuestion::getId).collect(Collectors.toSet());
        Set<Long> answeredQuestionIds = new HashSet<>();

        int score = 0;
        for (QuizSubmissionAnswer answer : answers) {
            Long questionId = answer.questionId();
            if (!quizQuestionIds.contains(questionId)) {
                throw new BadRequestException("Answer references invalid question: " + questionId);
            }
            if (!answeredQuestionIds.add(questionId)) {
                throw new BadRequestException("Duplicate answer for question: " + questionId);
            }
            AnswerOption chosen = optionsByQuestion.getOrDefault(questionId, List.of()).stream()
                    .filter(option -> Objects.equals(option.getId(), answer.answerId()))
                    .findFirst()
                    .orElseThrow(() -> new BadRequestException("Invalid answer option for question: " + questionId));
            if (Boolean.TRUE.equals(chosen.getCorrect())) {
                score++;
            }
        }
        return score;
    }

    private Map<Long, List<AnswerOption>> loadOptionsByQuestion(List<QuizQuestion> questions) {
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
        return answerOptionRepository.findByQuestion_IdIn(questionIds).stream()
                .collect(Collectors.groupingBy(option -> option.getQuestion().getId()));
    }

    private static void requireManager(Course course, User actor) {
        CourseAccess.requireManager(course, actor, NOT_STAFF, NOT_OWNER);
    }

    private void requireCanView(Quiz quiz, User viewer) {
        if (viewer.isStudent()) {
            requireEnrolled(quiz, viewer, "Enroll in the course to access this quiz");
        } else {
            requireManager(quiz.getCourse(), viewer);
        }
    }

    /**
     * Managers may always submit. Everyone else must be enrolled, which includes instructors
     * taking a course taught by someone else.
     */
    private void requireCanSubmit(Quiz quiz, User actor) {
        if (!quiz.getCourse().isManagedBy(actor)) {
            requireEnrolled(quiz, actor, "Enroll in the course before taking the quiz");
        }
    }

    private void requireEnrolled(Quiz quiz, User user, String message) {
        if (!enrollmentService.isStudentEnrolled(user.getId(), quiz.getCourse().getId())) {
            throw new ForbiddenException(message);
        }
    }
}
