package com.saasplatform.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Standard API response wrapper.
 * All endpoints return this structure for consistency.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        String message,
        T data,
        Object errors
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, "Created successfully", data, null);
    }

    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, null);
    }

    public static ApiResponse<Void> error(String message, Object errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}
