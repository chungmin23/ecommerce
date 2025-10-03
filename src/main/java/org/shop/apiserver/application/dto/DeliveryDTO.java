package org.shop.apiserver.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryDTO {

    private String receiverName;      // 수령인 이름
    private String receiverPhone;     // 수령인 전화번호
    private String address;           // 배송 주소
    private String zipCode;           // 우편번호
    private String deliveryMessage;   // 배송 메시지
}
