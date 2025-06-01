package com.anotation.anotation_be.common.enums;

import com.anotation.anotation_be.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    USER("ROLE_USER", "일반 사용자"),
    ADMIN("ROLE_ADMIN", "관리자"),;

    private final String key;
    private final String description;

    public static Role from(String value) throws BusinessException {
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.ROLE_INVALID);
        }
    }
}
