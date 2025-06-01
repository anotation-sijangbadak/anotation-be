package com.anotation.anotation_be.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonStatus {
    SUCCESS("S001", "요청이 성공적으로 처리되었습니다."),
    CREATED("S002", "요청이 성공적으로 생성되었습니다."),
    UPDATED("S003", "요청이 성공적으로 수정되었습니다."),
    DELETED("S004", "요청이 성공적으로 삭제되었습니다."),
    EMPTY("S005", "데이터가 존재하지 않습니다."),
    ALREADY_EXIST("S006", "이미 존재하지만 성공 처리 되었습니다.");

    private final String code;
    private final String message;
}
