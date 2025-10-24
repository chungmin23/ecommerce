package org.shop.apiserver.presentation.controller.advice;

import java.util.Map;
import java.util.NoSuchElementException;

import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.common.exception.CouponException;
import org.shop.apiserver.util.CustomJWTException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * CustomControllerAdvice
 */
@RestControllerAdvice
@Log4j2
public class CustomControllerAdvice {

    @ExceptionHandler(NoSuchElementException.class)
    protected ResponseEntity<?> notExist(NoSuchElementException e) {
        String msg = e.getMessage();
        log.warn("[Exception] NoSuchElementException: {}", msg);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("msg", msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    protected ResponseEntity<?> handleIllegalState(IllegalStateException e) {
        String msg = e.getMessage();
        log.warn("[Exception] IllegalStateException: {}", msg);
        
        // 재고 부족 메시지인 경우 410 상태 코드 반환
        if (msg.contains("소진") || msg.contains("재고")) {
            return ResponseEntity.status(HttpStatus.GONE).body(Map.of("msg", msg));
        }
        // 이미 발급 메시지인 경우 409 상태 코드 반환
        if (msg.contains("이미") || msg.contains("중복")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("msg", msg));
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("msg", msg));
    }

    @ExceptionHandler(CouponException.class)
    protected ResponseEntity<?> handleCouponException(CouponException e) {
        String msg = e.getMessage();
        HttpStatus status = e.getErrorCode().getStatus();
        log.warn("[Exception] CouponException: {} - {}", status, msg);
        
        return ResponseEntity.status(status).body(Map.of(
            "code", e.getErrorCode().getCode(),
            "msg", msg
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<?> handleIllegalArgumentException(MethodArgumentNotValidException e) {
        String msg = e.getMessage();
        log.warn("[Exception] MethodArgumentNotValidException: {}", msg);
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(Map.of("msg", msg));
    }

    @ExceptionHandler(CustomJWTException.class)
    protected ResponseEntity<?> handleJWTException(CustomJWTException e) {
        String msg = e.getMessage();
        log.warn("[Exception] CustomJWTException: {}", msg);
        return ResponseEntity.ok().body(Map.of("error", msg));
    }

    @ExceptionHandler(Exception.class)
    protected ResponseEntity<?> handleException(Exception e) {
        String msg = e.getMessage();
        log.error("[Exception] Unhandled Exception: {}", msg, e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("msg", "서버 오류가 발생했습니다: " + msg));
    }
}
