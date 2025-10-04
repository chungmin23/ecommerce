package org.shop.apiserver.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    
    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C003", "서버 오류가 발생했습니다."),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C004", "잘못된 타입입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "C005", "접근이 거부되었습니다."),
    
    // Entity
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "엔티티를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "E002", "상품을 찾을 수 없습니다."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "E003", "주문을 찾을 수 없습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "E004", "회원을 찾을 수 없습니다."),
    PAYMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "E005", "결제 정보를 찾을 수 없습니다."),
    
    // Business
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "B001", "이미 사용중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "B002", "비밀번호가 일치하지 않습니다."),
    INSUFFICIENT_STOCK(HttpStatus.BAD_REQUEST, "B003", "재고가 부족합니다."),
    CANNOT_CANCEL_ORDER(HttpStatus.BAD_REQUEST, "B004", "취소할 수 없는 주문 상태입니다."),
    ALREADY_PAID_ORDER(HttpStatus.BAD_REQUEST, "B005", "이미 결제된 주문입니다."),
    PAYMENT_FAILED(HttpStatus.BAD_REQUEST, "B006", "결제 처리에 실패했습니다."),
    
    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A004", "리프레시 토큰을 찾을 수 없습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "A005", "유효하지 않은 리프레시 토큰입니다.");
    
    private final HttpStatus status;
    private final String code;
    private final String message;
}
