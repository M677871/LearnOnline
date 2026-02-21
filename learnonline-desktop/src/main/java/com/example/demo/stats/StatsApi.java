package com.example.demo.stats;

import com.example.demo.common.ApiClient;
import com.example.demo.common.ApiResponse;
import com.example.demo.model.ChartPoint;
import com.fasterxml.jackson.core.type.TypeReference;

/**
 * HTTP client for statistics endpoints powering charts/3D visualizations.
 */
public class StatsApi {

    private final ApiClient client = new ApiClient();

    /**
     * Returns quiz average scores (percent 0-100) for the given course.
     *
     * @param courseId course identifier
     * @return array of chart points (quiz name + average score)
     */
    public ChartPoint[] quizAverages(Long courseId) {
        ApiResponse<ChartPoint[]> resp = client.get(
                "/api/statistics/courses/" + courseId + "/quiz-averages",
                new TypeReference<ChartPoint[]>() {});
        return resp.getBody();
    }
}
