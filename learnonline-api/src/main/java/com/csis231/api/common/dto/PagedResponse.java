package com.csis231.api.common.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Stable JSON envelope for paginated results, independent of Spring Data's {@link Page} serialization.
 */
public record PagedResponse<T>(
        List<T> content,
        int number,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static <T> PagedResponse<T> fromPage(Page<T> page) {
        return new PagedResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
