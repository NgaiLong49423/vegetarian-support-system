package tech.mamxanh.common.response;

import java.util.List;

/** Bounded page response shared by list endpoints. */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <T> PageResponse<T> of(List<T> content, int page, int size, long total) {
        return new PageResponse<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }
}
