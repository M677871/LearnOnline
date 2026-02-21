package com.example.demo.model;

/**
 * DTO representing a single chart point returned by statistics endpoints.
 *
 * @param label display label (e.g., quiz name)
 * @param value numeric value (percentage or count)
 *
 * <p>Used by 2D/3D visualizations for quiz averages or enrollments.</p>
 */
public record ChartPoint(String label, double value) { }
