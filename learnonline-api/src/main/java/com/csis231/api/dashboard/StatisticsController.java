package com.csis231.api.dashboard;

import com.csis231.api.user.User;
import com.csis231.api.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Read-only statistics endpoints used by JavaFX visualizations.
 */
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final UserRepository userRepository;
    private final StatisticsService statisticsService;

    /**
     * Returns average score per quiz for a course (percent 0-100).
     * Accessible by admins or the instructor who owns the course.
     *
     * @param courseId course identifier
     * @param authentication authenticated principal
     * @return list of chart points (quiz name + average score)
     */
    @GetMapping("/courses/{courseId}/quiz-averages")
    public List<ChartPoint> quizAverages(@PathVariable Long courseId, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new com.csis231.api.common.UnauthorizedException("Authentication required");
        }
        User actor = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new com.csis231.api.common.ResourceNotFoundException("User not found: " + authentication.getName()));
        return statisticsService.quizAveragesForCourse(courseId, actor);
    }
}
