package org.shop.apiserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.dto.PaymentResponseDTO;
import org.shop.apiserver.service.PaymentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Log4j2
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * 결제 조회 (paymentId)
     * GET /api/payments/{paymentId}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/{paymentId}")
    public PaymentResponseDTO getPayment(
            @PathVariable Long paymentId,
            Principal principal) {

        log.info("Get payment: " + paymentId);

        return paymentService.getPayment(paymentId);
    }

    /**
     * 주문번호로 결제 조회
     * GET /api/payments/order/{orderNumber}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/order/{orderNumber}")
    public PaymentResponseDTO getPaymentByOrderNumber(
            @PathVariable String orderNumber,
            Principal principal) {

        log.info("Get payment by order: " + orderNumber);

        return paymentService.getPaymentByOrderNumber(orderNumber);
    }

    /**
     * 결제 취소 (주문 취소 시 자동 호출)
     * POST /api/payments/cancel/{orderNumber}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @PostMapping("/cancel/{orderNumber}")
    public Map<String, String> cancelPayment(
            @PathVariable String orderNumber,
            @RequestBody Map<String, String> request,
            Principal principal) {

        String cancelReason = request.getOrDefault("cancelReason", "사용자 요청");

        log.info("Cancel payment for order: " + orderNumber);

        paymentService.cancelPayment(orderNumber, cancelReason);

        return Map.of("result", "SUCCESS");
    }
}
