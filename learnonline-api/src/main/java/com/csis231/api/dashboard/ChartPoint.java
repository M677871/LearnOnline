package com.csis231.api.dashboard;

/**
 * Simple DTO used for chart / visualization points.
 *
 * @param label human-readable label for the point (e.g., quiz name)
 * @param value numeric value (e.g., average score or count)
 */
public record ChartPoint(String label, double value) { }
