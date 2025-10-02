package org.shop.apiserver.domain;

public enum DeliveryStatus {
    READY("배송 준비중"),
    SHIPPING("배송중"),
    COMPLETED("배송 완료");

    private final String description;

    DeliveryStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
