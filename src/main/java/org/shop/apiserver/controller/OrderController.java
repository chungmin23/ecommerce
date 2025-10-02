package org.shop.apiserver.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.dto.*;
import org.shop.apiserver.service.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Log4j2
public class OrderController {

    private final OrderService orderService;

    /**
     * 주문 생성
     * POST /api/orders/
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @PostMapping("/")
    public Map<String, String> createOrder(
            @RequestBody OrderDTO orderDTO,
            Principal principal) {

        log.info("Create order request: " + orderDTO);

        // 로그인한 사용자의 이메일 설정
        orderDTO.setEmail(principal.getName());

        String orderNumber = orderService.createOrder(orderDTO);

        return Map.of("orderNumber", orderNumber, "result", "SUCCESS");
    }

    /**
     * 주문 상세 조회
     * GET /api/orders/{ono}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/{ono}")
    public OrderResponseDTO getOrder(
            @PathVariable Long ono,
            Principal principal) {

        log.info("Get order: " + ono);

        return orderService.getOrder(ono, principal.getName());
    }

    /**
     * 주문번호로 조회
     * GET /api/orders/number/{orderNumber}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/number/{orderNumber}")
    public OrderResponseDTO getOrderByNumber(
            @PathVariable String orderNumber,
            Principal principal) {

        log.info("Get order by number: " + orderNumber);

        return orderService.getOrderByOrderNumber(orderNumber, principal.getName());
    }

    /**
     * 내 주문 목록 조회
     * GET /api/orders/my
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @GetMapping("/my")
    public PageResponseDTO<OrderListDTO> getMyOrders(
            PageRequestDTO pageRequestDTO,
            Principal principal) {

        log.info("Get my orders: " + principal.getName());

        return orderService.getMyOrders(principal.getName(), pageRequestDTO);
    }

    /**
     * 주문 취소
     * DELETE /api/orders/{ono}
     */
    @PreAuthorize("hasAnyRole('ROLE_USER')")
    @DeleteMapping("/{ono}")
    public Map<String, String> cancelOrder(
            @PathVariable Long ono,
            Principal principal) {

        log.info("Cancel order: " + ono);

        orderService.cancelOrder(ono, principal.getName());

        return Map.of("result", "SUCCESS");
    }

    /**
     * 주문 상태 변경 (관리자용)
     * PUT /api/orders/{ono}/status
     */
    @PreAuthorize("hasAnyRole('ROLE_ADMIN')")
    @PutMapping("/{ono}/status")
    public Map<String, String> updateOrderStatus(
            @PathVariable Long ono,
            @RequestBody Map<String, String> request) {

        String status = request.get("status");

        log.info("Update order status: " + ono + " -> " + status);

        orderService.updateOrderStatus(ono, status);

        return Map.of("result", "SUCCESS");
    }
}