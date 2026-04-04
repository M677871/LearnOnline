package com.csis231.api.dashboard.controller;

import com.csis231.api.common.exception.ResourceNotFoundException;
import com.csis231.api.common.exception.UnauthorizedException;
import com.csis231.api.common.service.AuthenticatedUserService;
import com.csis231.api.dashboard.dto.ChartPoint;
import com.csis231.api.dashboard.service.StatisticsService;
import com.csis231.api.user.model.User;
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

    private final AuthenticatedUserService authenticatedUserService;
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
        User actor = authenticatedUserService.require(authentication);
        return statisticsService.quizAveragesForCourse(courseId, actor);
    }
}
