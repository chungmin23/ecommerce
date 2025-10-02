package org.shop.apiserver.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.shop.apiserver.domain.DeliveryStatus;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeliveryResponseDTO {

    private Long dno;
    private String receiverName;
    private String receiverPhone;
    private String address;
    private String zipCode;
    private String deliveryMessage;
    private DeliveryStatus status;
    private String trackingNumber;
}

