package org.shop.apiserver.common.exception;

/**
 * 엔티티를 찾을 수 없을 때 발생하는 예외
 */
public class EntityNotFoundException extends BusinessException {
    
    public EntityNotFoundException(ErrorCode errorCode) {
        super(errorCode);
    }
    
    public EntityNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }
    
    // 편의 메서드
    public static EntityNotFoundException product(Long productId) {
        return new EntityNotFoundException(
            ErrorCode.PRODUCT_NOT_FOUND, 
            "상품을 찾을 수 없습니다. ID: " + productId
        );
    }
    
    public static EntityNotFoundException order(Long orderId) {
        return new EntityNotFoundException(
            ErrorCode.ORDER_NOT_FOUND, 
            "주문을 찾을 수 없습니다. ID: " + orderId
        );
    }
    
    public static EntityNotFoundException member(String email) {
        return new EntityNotFoundException(
            ErrorCode.MEMBER_NOT_FOUND, 
            "회원을 찾을 수 없습니다. Email: " + email
        );
    }
    
    public static EntityNotFoundException payment(Long paymentId) {
        return new EntityNotFoundException(
            ErrorCode.PAYMENT_NOT_FOUND, 
            "결제 정보를 찾을 수 없습니다. ID: " + paymentId
        );
    }
}
