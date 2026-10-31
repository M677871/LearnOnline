package com.csis231.api.common.util;

import com.csis231.api.common.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class Paging {

    private Paging() {
    }

    /**
     * Builds a {@link Pageable} from raw query parameters. Negative pages fall back to the first page,
     * while a non-positive size is rejected because it has no sensible meaning.
     */
    public static Pageable of(int page, int size) {
        if (size <= 0) {
            throw new BadRequestException("Size must be greater than zero");
        }
        return PageRequest.of(Math.max(0, page), size);
    }
}
