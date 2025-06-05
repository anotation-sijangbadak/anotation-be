package com.anotation.anotation_be.common.dto.global;

import com.anotation.anotation_be.common.enums.CommonStatus;
import com.anotation.anotation_be.common.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "공통 API 응답")
public class CommonResponse<T> {
    @Schema(description = "성공 여부", example = "true")
    private final boolean success;

    @Schema(description = "응답 데이터")
    private final T data;

    @Schema(description = "응답 메시지")
    private final String message;

    @Schema(description = "응답 코드")
    private final String code;

    public static <T> CommonResponse<T> ok(T data) {
        return CommonResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    public static <T> CommonResponse<T> ok(CommonStatus status, T data) {
        return CommonResponse.<T>builder()
                .success(true)
                .data(data)
                .message(status.getMessage())
                .code(status.getCode())
                .build();
    }

    public static <T> CommonResponse<T> ok(CommonStatus status) {
        return ok(status, null);
    }

    public static <T> CommonResponse<T> fail(ErrorCode errorCode) {
        return CommonResponse.<T>builder()
                .success(false)
                .message(errorCode.getMessage())
                .code(errorCode.getCode())
                .build();
    }

    public static <T> CommonResponse<T> fail(ErrorCode errorCode, String message) {
        return CommonResponse.<T>builder()
                .success(false)
                .message(message)
                .code(errorCode.getCode())
                .build();
    }

    public static <T> CommonResponse<T> fail(String code, String message) {
        return CommonResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }
}
