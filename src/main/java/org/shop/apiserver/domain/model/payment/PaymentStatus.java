package org.shop.apiserver.domain.model.payment;

public enum PaymentStatus {

    READY("결제 준비"),           // 결제 위젯 호출 전
    IN_PROGRESS("결제 진행중"),   // 결제 위젯에서 결제 중
    DONE("결제 완료"),            // 결제 승인 완료
    CANCELLED("결제 취소"),       // 결제 취소됨
    FAILED("결제 실패");          // 결제 실패

    private final String description;

    PaymentStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
