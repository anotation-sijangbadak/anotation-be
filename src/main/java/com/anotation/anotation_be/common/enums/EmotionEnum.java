package com.anotation.anotation_be.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmotionEnum {
    ADMIRATION("감탄"),
    AMUSEMENT("즐거움"),
    ANGER("분노"),
    ANNOYANCE("짜증남"),
    APPROVAL("인정"),
    CARING("보살핌"),
    CONFUSION("혼란"),
    CURIOSITY("호기심"),
    DESIRE("욕구"),
    DISAPPOINTMENT("실망"),
    DISAPPROVAL("반감"),
    DISGUST("혐오"),
    EMBARRASSMENT("어색함"),
    EXCITEMENT("흥분"),
    FEAR("공포"),
    GRATITUDE("감사"),
    GRIEF("비탄"),
    JOY("기쁨"),
    LOVE("애정"),
    NERVOUSNESS("불안"),
    OPTIMISM("낙관"),
    PRIDE("자랑스러움"),
    REALIZATION("깨달음"),
    RELIEF("안도"),
    REMORSE("회한"),
    SADNESS("슬픔"),
    SURPRISE("놀라움"),
    NEUTRAL("중립적");

   private final String emotion_kr;

}
