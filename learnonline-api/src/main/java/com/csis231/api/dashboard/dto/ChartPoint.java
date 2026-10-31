package com.csis231.api.dashboard.dto;

/**
 * One point of a client chart.
 *
 * @param label for example a quiz name
 * @param value for example an average score
 */
public record ChartPoint(String label, double value) {
}
