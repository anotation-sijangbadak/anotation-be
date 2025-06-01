package com.anotation.anotation_be.common.enums;

import com.anotation.anotation_be.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@AllArgsConstructor
public enum Genre {
    HIPHOP(1),
    JAZZ(2),
    JPOP(4);

    private final int value;

    public static int fromGenre(List<String> input) {
        try{
            return input.stream()
                    .map(String::toUpperCase)
                    .map(Genre::valueOf)
                    .mapToInt(Genre::getValue)
                    .reduce(0, (a, b) -> a | b);
        } catch (Exception e){
            throw new BusinessException(ErrorCode.INVALID_GENRE);
        }
    }

    public static List<Genre> toGenre(int value) {
        return Arrays.stream(Genre.values())
                .filter(genre -> (value & genre.getValue()) != 0)
                .collect(Collectors.toList());
    }
}
