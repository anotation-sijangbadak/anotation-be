package com.anotation.anotation_be.common.exception;

import com.anotation.anotation_be.common.dto.global.CommonResponse;
import com.anotation.anotation_be.common.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.io.IOException;

@RestControllerAdvice
@Slf4j
public class CustomErrorHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(final BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn(errorCode.getMessage());
        return new ResponseEntity<>(CommonResponse.fail(errorCode), HttpStatus.valueOf(errorCode.getStatus()));
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<?> handleIOException(final IOException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(CommonResponse.fail("C999", "입출력 과정에서 오류가 발생했습니다."), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationException(final MethodArgumentNotValidException e) {
        ErrorCode errorCode = ErrorCode.VALIDATION_CHECK_FAILED;
        log.warn(errorCode.getMessage());
        return new ResponseEntity<>(CommonResponse.fail(errorCode), HttpStatus.valueOf(errorCode.getStatus()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeException(final RuntimeException e) {
        log.warn(e.getMessage());
        return new ResponseEntity<>(CommonResponse.fail("C999", "이게 뭔 에러여 헤응"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
