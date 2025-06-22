package com.anotation.anotation_be.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    INVALID_INPUT(400, "X001","잘못된 입력 값입니다."),
    USER_NOT_FOUND(404, "X001","사용자를 찾을 수 없습니다."),
    INTERNAL_ERROR(500, "X001", "서버 내부 오류입니다"),
    UNAUTHORIZED(401, "X001","로그인이 필요합니다."),
    FORBIDDEN(403, "X002", "권한이 없습니다"),
    NO_TOKEN(403, "X003", "토큰이 없습니다."),
    TOKEN_EXPIRED(403, "X004", "토큰이 만료되었습니다."),
    TOKEN_INVALID(403, "X005", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(403, "X006", "리프레시 토큰이 만료되었습니다."),
    ROLE_INVALID(401, "X007", "유효하지 않은 역할입니다."),
    INVALID_GENRE(400, "X002", "잘못된 장르 입력입니다."),
    EMAIL_ALREADY_EXIST(409, "X003", "이미 존재하는 이메일입니다." ),
    NICKNAME_ALREADY_EXIST(409, "X004", "이미 존재하는 닉네임입니다."),
    VALIDATION_CHECK_FAILED(400, "X001", "Request Body의 유효성 검증에 실패하였습니다."),
    LOGIN_FAILED(400, "X008", "로그인에 실패하였습니다."),
    EMAIL_SEND_FAILED(500, "X002", "이메일 전송에 실패했습니다."),
    EMAIL_SERVER_ERROR(500, "X003", "메일 서버의 오류로 이메일 전송에 실패했습니다."),
    RABBITMQ_ERROR(500, "X001", "MQ 메시지 수신에 실패하였습니다."),
    SPOTIFY_TOO_MANY_REQUEST(500, "X002", "Spotify API 호출이 너무 많습니다."),
    EXTERNAL_API_REQUEST_ERROR(500, "X003", "외부 API에서 4XX 응답을 전달하였습니다."),
    EXTERNAL_API_SERVER_ERROR(503, "X004", "외부 API에서 5XX 응답을 전달하였습니다."),
    ;

    private final int status;
    private final String code;
    private final String message;
}

/*
 * ErrorCode
 * ##### 4XX #####
 * 400 Bad Request : 잘못된 요청
 * 401 Unauthorized : 권한 없음
 * 402 Payment Required (deprecated) : 결제 필요
 * 403 Forbidden : 거부됨
 * 404 Not Found : 찾을 수 없음
 * 405 Method Not Allowed : 허용되지 않은 방법 (PUT, DELETE에서 허용하지 않은 메소드로 접근 시)
 * 406 Not Acceptable : 받아들일 수 없음 (웹 방화벽 문제)
 * 407 Proxy Authentication Required : 프록시 인증 필요
 * 408 Request Timeout : 요청 시간 초과
 * 409 Conflict : 충돌 (사용자의 요청이 서버의 상태와 충돌)
 * 429 Too Many Request : 너무 많은 요청
 *
 * ##### 5XX #####
 * 500 Internal Server Error : 내부 서버 오류
 * 502 Bad Gateway : 게이트웨이 불량
 * 503 Service Temporarily Unavailable : 일시적으로 서비스를 이용할 수 없음
 * 504 Gateway Timeout : 게이트웨이 시간 초과
 */
