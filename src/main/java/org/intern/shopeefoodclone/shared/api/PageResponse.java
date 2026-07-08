package org.intern.shopeefoodclone.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import org.springframework.data.domain.Page;

import java.util.List;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PageResponse<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages,
        boolean last,
        boolean first
) {
    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }

    public static <T, R> PageResponse<R> of(Page<T> page, List<R> content) {
        return PageResponse.<R>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .first(page.isFirst())
                .build();
    }
}
