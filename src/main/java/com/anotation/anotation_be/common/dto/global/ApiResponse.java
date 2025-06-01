package com.anotation.anotation_be.common.dto.global;

import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.common.enums.ErrorCode;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
    private final String code;

    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(CommonStatus status, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(status.getMessage())
                .code(status.getCode())
                .build();
    }

    public static <T> ApiResponse<T> ok(CommonStatus status) {
        return ok(status, null);
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();
    }

    public static <T> ApiResponse<T> fail(String code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }
}
