package com.csis231.api.quiz;

import com.csis231.api.common.BadRequestException;
import com.csis231.api.common.ResourceNotFoundException;
import com.csis231.api.common.UnauthorizedException;
import com.csis231.api.course.Course;
import com.csis231.api.course.CourseRepository;
import com.csis231.api.enrollment.EnrollmentService;
import com.csis231.api.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service encapsulating quiz authoring, delivery and grading.
 */
@Service
@RequiredArgsConstructor
public class QuizService {
    private final QuizRepository quizRepository;
    private final QuizQuestionRepository questionRepository;
    private final AnswerOptionRepository answerOptionRepository;
    private final QuizResultRepository resultRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentService enrollmentService;

    /**
     * Creates a new quiz under the given course for the instructor/admin actor.
     *
     * @param req   the quiz creation payload
     * @param actor the authenticated user performing the action
     * @return the persisted {@link Quiz}
     * @throws BadRequestException        if the payload is missing
     * @throws ResourceNotFoundException  if the course does not exist
     * @throws UnauthorizedException      if the actor is not permitted
     */
    @Transactional
    public Quiz createQuiz(QuizCreateRequest req, User actor) {
        requireInstructorOrAdmin(actor);
        Course course = courseRepository.findById(req.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + req.courseId()));
        ensureCourseOwnership(actor, course);

        Quiz quiz = Quiz.builder()
                .course(course)
                .name(req.name())
                .description(req.description())
                .build();
        return quizRepository.save(quiz);
    }

    /**
     * Adds questions (and their answer options) to an existing quiz.
     *
     * @param quizId   the quiz identifier
     * @param requests the list of questions with answers to create
     * @param actor    the authenticated user performing the action
     * @return the list of created {@link QuizQuestion} entities
     * @throws BadRequestException        if payload is invalid
     * @throws ResourceNotFoundException  if the quiz is not found
     * @throws UnauthorizedException      if the actor cannot modify the quiz
     */
    @Transactional
    public List<QuizQuestion> addQuestions(Long quizId, List<QuizQuestionRequest> requests, User actor) {
        if (quizId == null) throw new BadRequestException("quizId required");
        if (requests == null || requests.isEmpty()) throw new BadRequestException("Questions are required");
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        ensureCourseOwnership(actor, quiz.getCourse());

        List<QuizQuestion> createdQuestions = new ArrayList<>();
        for (QuizQuestionRequest request : requests) {
            boolean hasCorrect = request.answers().stream().anyMatch(AnswerCreateRequest::correct);
            if (!hasCorrect) {
                throw new BadRequestException("Each question must have at least one correct answer");
            }
            QuizQuestion question = QuizQuestion.builder()
                    .quiz(quiz)
                    .questionText(request.questionText())
                    .build();
            QuizQuestion saved = questionRepository.save(question);
            createdQuestions.add(saved);

            for (AnswerCreateRequest answerReq : request.answers()) {
                AnswerOption option = AnswerOption.builder()
                        .question(saved)
                        .answerText(answerReq.answerText())
                        .correct(answerReq.correct())
                        .build();
                answerOptionRepository.save(option);
            }
        }
        return createdQuestions;
    }

    /**
     * Retrieves quiz details (questions and options) visible to the viewer.
     *
     * @param quizId the quiz identifier
     * @param viewer the requesting user
     * @return a {@link QuizDetailDto} containing quiz content
     * @throws ResourceNotFoundException if the quiz is not found
     * @throws UnauthorizedException     if the viewer cannot access the quiz
     */
    @Transactional(readOnly = true)
    public QuizDetailDto getQuizDetail(Long quizId, User viewer) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        enforceQuizAccessForView(quiz, viewer);

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
        Map<Long, List<AnswerOption>> optionsByQuestion = answerOptionRepository.findByQuestion_IdIn(questionIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getQuestion().getId()));

        List<QuizQuestionDto> questionDtos = questions.stream().map(q -> {
            List<AnswerOptionDto> options = optionsByQuestion.getOrDefault(q.getId(), Collections.emptyList())
                    .stream()
                    .map(opt -> new AnswerOptionDto(opt.getId(), opt.getAnswerText()))
                    .toList();
            return new QuizQuestionDto(q.getId(), q.getQuestionText(), options);
        }).toList();

        return QuizMapper.toDetailDto(quiz, questionDtos);
    }

    /**
     * Submits answers for a quiz, validates enrollment/ownership, and computes score.
     *
     * @param quizId  the quiz identifier
     * @param request the submission payload containing answers
     * @param actor   the authenticated user submitting
     * @return a {@link QuizSubmissionResponse} summarizing the result
     * @throws UnauthorizedException    if the user cannot submit
     * @throws BadRequestException      if answers reference invalid questions/options
     * @throws ResourceNotFoundException if the quiz is not found
     */
    @Transactional
    public QuizSubmissionResponse submitQuiz(Long quizId, QuizSubmissionRequest request, User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() != User.Role.STUDENT && actor.getRole() != User.Role.ADMIN && actor.getRole() != User.Role.INSTRUCTOR) {
            throw new UnauthorizedException("Not authorized to submit quizzes");
        }
        if (request == null || request.answers() == null || request.answers().isEmpty()) {
            throw new BadRequestException("Answers are required");
        }
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));

        if (actor.getRole() == User.Role.STUDENT || actor.getRole() == User.Role.INSTRUCTOR) {
            boolean ownsCourse = actor.getRole() == User.Role.INSTRUCTOR
                    && quiz.getCourse() != null
                    && quiz.getCourse().getInstructor() != null
                    && Objects.equals(quiz.getCourse().getInstructor().getId(), actor.getId());
            if (!ownsCourse) {
                boolean enrolled = enrollmentService.isStudentEnrolled(actor.getId(), quiz.getCourse().getId());
                if (!enrolled) throw new UnauthorizedException("Enroll in the course before taking the quiz");
            }
        }

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        Map<Long, QuizQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(QuizQuestion::getId, q -> q));
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();
        Map<Long, List<AnswerOption>> optionsByQuestion = answerOptionRepository.findByQuestion_IdIn(questionIds).stream()
                .collect(Collectors.groupingBy(opt -> opt.getQuestion().getId()));

        int totalQuestions = questions.size();
        int score = 0;

        for (QuizSubmissionAnswer answer : request.answers()) {
            QuizQuestion question = questionMap.get(answer.questionId());
            if (question == null) {
                throw new BadRequestException("Answer references invalid question: " + answer.questionId());
            }
            List<AnswerOption> options = optionsByQuestion.getOrDefault(question.getId(), Collections.emptyList());
            Optional<AnswerOption> chosen = options.stream()
                    .filter(opt -> Objects.equals(opt.getId(), answer.answerId()))
                    .findFirst();
            if (chosen.isEmpty()) {
                throw new BadRequestException("Invalid answer option for question: " + question.getId());
            }
            if (Boolean.TRUE.equals(chosen.get().getCorrect())) {
                score++;
            }
        }

        QuizResult result = QuizResult.builder()
                .quiz(quiz)
                .student(actor)
                .score(score)
                .totalQuestions(totalQuestions)
                .build();
        QuizResult saved = resultRepository.save(result);

        double percentage = totalQuestions == 0 ? 0 : (score * 100.0 / totalQuestions);
        return new QuizSubmissionResponse(
                quiz.getId(),
                actor.getId(),
                score,
                totalQuestions,
                percentage,
                saved.getCompletedAt()
        );
    }

    /**
     * Retrieves all results for a quiz, enforcing course ownership for instructors.
     *
     * @param quizId the quiz identifier
     * @param actor  the authenticated user requesting the data
     * @return list of {@link QuizResultDto} for the quiz
     * @throws ResourceNotFoundException if the quiz is not found
     * @throws UnauthorizedException     if actor cannot view results
     */
    @Transactional(readOnly = true)
    public List<QuizResultDto> resultsForQuiz(Long quizId, User actor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        ensureCourseOwnership(actor, quiz.getCourse());
        return resultRepository.findByQuiz_Id(quizId).stream()
                .map(QuizMapper::toResultDto)
                .collect(Collectors.toList());
    }

    /**
     * Deletes a quiz along with its questions, answers, and results.
     *
     * @param quizId the quiz identifier
     * @param actor  the authenticated user performing deletion
     * @throws ResourceNotFoundException if the quiz is not found
     * @throws UnauthorizedException     if actor cannot manage the course
     */
    @Transactional
    public void deleteQuiz(Long quizId, User actor) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        ensureCourseOwnership(actor, quiz.getCourse());

        List<QuizQuestion> questions = questionRepository.findByQuiz_Id(quizId);
        List<Long> questionIds = questions.stream().map(QuizQuestion::getId).toList();

        if (!questionIds.isEmpty()) {
            var options = answerOptionRepository.findByQuestion_IdIn(questionIds);
            answerOptionRepository.deleteAll(options);
            questionRepository.deleteAll(questions);
        }
        var results = resultRepository.findByQuiz_Id(quizId);
        resultRepository.deleteAll(results);

        quizRepository.delete(quiz);
    }

    /**
     * Returns the latest quiz results for a student (limited to recent entries).
     *
     * @param studentId the student identifier
     * @return list of recent {@link QuizResultDto}
     */
    @Transactional(readOnly = true)
    public List<QuizResultDto> latestResultsForStudent(Long studentId) {
        return resultRepository.findTop5ByStudent_IdOrderByCompletedAtDesc(studentId).stream()
                .map(QuizMapper::toResultDto)
                .collect(Collectors.toList());
    }

    /**
     * Returns the most recent result for the given quiz and user, if accessible.
     *
     * @param quizId the quiz identifier
     * @param actor  the authenticated user requesting their result
     * @return the latest {@link QuizResultDto} or {@code null} if none
     * @throws UnauthorizedException    if the user cannot view the quiz
     * @throws ResourceNotFoundException if the quiz is not found
     */
    @Transactional(readOnly = true)
    public QuizResultDto latestResultForUser(Long quizId, User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz not found: " + quizId));
        // any authenticated user can ask for their own result if they can view the quiz
        enforceQuizAccessForView(quiz, actor);
        return resultRepository.findTop1ByQuiz_IdAndStudent_IdOrderByCompletedAtDesc(quizId, actor.getId())
                .map(QuizMapper::toResultDto)
                .orElse(null);
    }

    private static void requireInstructorOrAdmin(User actor) {
        if (actor == null) throw new UnauthorizedException("Authentication required");
        if (actor.getRole() == User.Role.ADMIN || actor.getRole() == User.Role.INSTRUCTOR) return;
        throw new UnauthorizedException("Only instructors or admins can manage quizzes");
    }

    private static void ensureCourseOwnership(User actor, Course course) {
        requireInstructorOrAdmin(actor);
        if (actor.getRole() == User.Role.ADMIN) return;
        if (course == null || course.getInstructor() == null || !Objects.equals(course.getInstructor().getId(), actor.getId())) {
            throw new UnauthorizedException("You can only manage quizzes for your own courses");
        }
    }

    private void enforceQuizAccessForView(Quiz quiz, User viewer) {
        if (viewer == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (viewer.getRole() == User.Role.ADMIN) return;
        if (viewer.getRole() == User.Role.INSTRUCTOR) {
            ensureCourseOwnership(viewer, quiz.getCourse());
            return;
        }
        if (viewer.getRole() == User.Role.STUDENT) {
            boolean enrolled = enrollmentService.isStudentEnrolled(viewer.getId(), quiz.getCourse().getId());
            if (!enrolled) {
                throw new UnauthorizedException("Enroll in the course to access this quiz");
            }
            return;
        }
        throw new UnauthorizedException("Not authorized");
    }
}
