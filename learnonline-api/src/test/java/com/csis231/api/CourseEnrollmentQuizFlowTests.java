package com.csis231.api;

import com.csis231.api.category.model.Category;
import com.csis231.api.category.repository.CategoryRepository;
import com.csis231.api.course.model.Course;
import com.csis231.api.course.dto.CourseRequest;
import com.csis231.api.course.service.CourseService;
import com.csis231.api.enrollment.dto.EnrollmentRequest;
import com.csis231.api.enrollment.service.EnrollmentService;
import com.csis231.api.quiz.dto.QuizSubmissionRequest;

import com.csis231.api.quiz.dto.*;
import com.csis231.api.quiz.model.Quiz;
import com.csis231.api.quiz.repository.AnswerOptionRepository;
import com.csis231.api.quiz.repository.QuizQuestionRepository;
import com.csis231.api.quiz.service.QuizService;
import com.csis231.api.user.model.User;
import com.csis231.api.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {ApiApplication.class, TestMailConfig.class})
@Import(TestMailConfig.class)
@Transactional
class CourseEnrollmentQuizFlowTests {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private CourseService courseService;
    @Autowired
    private EnrollmentService enrollmentService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizQuestionRepository questionRepository;
    @Autowired
    private AnswerOptionRepository answerOptionRepository;

    @Test
    void studentCanEnrollCreateAndSubmitQuiz() {
        User instructor = userRepository.save(User.builder()
                .username("instr")
                .email("instr@example.com")
                .password("password1")
                .role(User.Role.INSTRUCTOR)
                .build());

        User student = userRepository.save(User.builder()
                .username("stud")
                .email("stud@example.com")
                .password("password2")
                .role(User.Role.STUDENT)
                .build());

        Category category = categoryRepository.save(Category.builder().name("Tech").build());

        Course course = courseService.createCourse(
                new CourseRequest("Course 101", "Desc", category.getId(), true),
                instructor);

        enrollmentService.enroll(student, new EnrollmentRequest(null, course.getId()));

        Quiz quiz = quizService.createQuiz(new QuizCreateRequest(course.getId(), "Quiz 1", "Basics"), instructor);
        quizService.addQuestions(quiz.getId(),
                List.of(new QuizQuestionRequest("What is 2+2?",
                        List.of(
                                new AnswerCreateRequest("3", false),
                                new AnswerCreateRequest("4", true)
                        ))),
                instructor);

        var question = questionRepository.findByQuiz_Id(quiz.getId()).get(0);
        var correctAnswer = answerOptionRepository.findByQuestion_IdIn(List.of(question.getId()))
                .stream()
                .filter(a -> Boolean.TRUE.equals(a.getCorrect()))
                .findFirst()
                .orElseThrow();

        QuizSubmissionResponse response = quizService.submitQuiz(
                quiz.getId(),
                new QuizSubmissionRequest(List.of(new QuizSubmissionAnswer(question.getId(), correctAnswer.getId()))),
                student);

        assertThat(response.score()).isEqualTo(1);
        assertThat(response.totalQuestions()).isEqualTo(1);
        assertThat(response.percentage()).isEqualTo(100.0);
    }
}
