package com.csis231.api.dashboard.controller;

import com.csis231.api.dashboard.dto.ChartPoint;
import com.csis231.api.dashboard.service.StatisticsService;
import com.csis231.api.user.service.AuthenticatedUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final AuthenticatedUserService authenticatedUserService;
    private final StatisticsService statisticsService;

    @GetMapping("/courses/{courseId}/quiz-averages")
    public List<ChartPoint> quizAverages(@PathVariable Long courseId, Authentication authentication) {
        return statisticsService.quizAveragesForCourse(courseId, authenticatedUserService.require(authentication));
    }
}
