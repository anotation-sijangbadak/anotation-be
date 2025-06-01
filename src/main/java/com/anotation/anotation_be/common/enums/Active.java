package com.anotation.anotation_be.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Active {
    ENABLED("활성화 상태"),
    DISABLED("비활성화 상태");

    private final String description;
}
