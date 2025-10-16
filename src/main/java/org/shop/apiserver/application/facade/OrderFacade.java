package org.shop.apiserver.application.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.*;
import org.shop.apiserver.application.service.PaymentService;
import org.shop.apiserver.domain.model.cart.CartItem;
import org.shop.apiserver.domain.model.delivery.Delivery;
import org.shop.apiserver.domain.model.delivery.DeliveryStatus;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.domain.model.order.OrderItem;
import org.shop.apiserver.domain.model.order.OrderStatus;
import org.shop.apiserver.domain.model.order.Orders;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * Order Facade
 * 
 * 역할:
 * - 복잡한 주문 생성 프로세스 조율
 * - 주문, 결제, 쿠폰, 상품, 장바구니 등 여러 서비스를 조합하여 일관된 주문 흐름 관리
 * - 트랜잭션 관리 및 예외 처리
 * 
 * 처리 흐름:
 * 1. 주문 검증 (회원, 상품, 쿠폰)
 * 2. 상품 재고 확인
 * 3. 주문 생성
 * 4. 배송 정보 등록
 * 5. 결제 처리
 * 6. 장바구니 비우기
 * 7. 로깅 및 예외 처리
 */
@Component
@Transactional
@RequiredArgsConstructor
@Log4j2
public class OrderFacade {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    
    private final PaymentService paymentService;

    /**
     * 주문 생성 + 결제 완료 (전체 흐름)
     * 
     * 처리 순서:
     * 1. 회원 확인
     * 2. 상품 및 재고 확인
     * 3. 쿠폰 할인 계산
     * 4. 주문 생성
     * 5. 배송 정보 저장
     * 6. 결제 처리
     * 7. 장바구니 정리
     * 
     * @param orderDTO 주문 정보
     * @param email 주문자 이메일
     * @return 주문번호
     * @throws IllegalArgumentException 검증 실패
     * @throws IllegalStateException 업무 로직 오류
     */
    public String checkoutAndPay(OrderDTO orderDTO, String email) {
        log.info("[OrderFacade] Checkout and Pay 시작 - email: {}", email);

        try {
            // 1. 회원 확인
            Member member = validateMember(email);
            log.info("[OrderFacade] 회원 확인 완료 - email: {}", email);

            // 2. 상품 및 재고 확인
            validateOrderItems(orderDTO.getOrderItems());
            log.info("[OrderFacade] 상품 및 재고 확인 완료");

            // 3. 총 금액 계산
            int totalAmount = calculateTotalAmount(orderDTO.getOrderItems());
            log.info("[OrderFacade] 총 금액 계산 - totalAmount: {}", totalAmount);

            // 4. 쿠폰 할인 적용 (나중에 구현)
            int discountAmount = 0;
            log.info("[OrderFacade] 할인 적용 - discountAmount: {}, finalAmount: {}", 
                    discountAmount, totalAmount);

            // 5. 주문 번호 생성
            String orderNumber = generateOrderNumber();
            log.info("[OrderFacade] 주문번호 생성 - orderNumber: {}", orderNumber);

            // 6. 주문 엔티티 생성
            Orders order = createOrderEntity(
                    orderNumber,
                    member,
                    orderDTO.getOrderItems(),
                    totalAmount,
                    discountAmount,
                    totalAmount
            );
            log.info("[OrderFacade] 주문 엔티티 생성 완료");

            // 7. 배송 정보 등록
            registerDelivery(order, orderDTO.getDelivery());
            log.info("[OrderFacade] 배송 정보 등록 완료");

            // 8. 주문 저장
            orderRepository.save(order);
            log.info("[OrderFacade] 주문 저장 완료 - ono: {}", order.getOno());

            // 9. 결제 처리
            String paymentMethod = orderDTO.getPaymentMethod() != null ?
                    orderDTO.getPaymentMethod() : "CARD";
            paymentService.processPayment(orderNumber, paymentMethod);
            log.info("[OrderFacade] 결제 처리 완료 - paymentMethod: {}", paymentMethod);

            // 10. 장바구니 비우기 (나중에 구현)
            log.info("[OrderFacade] 장바구니 정리 (생략)");

            log.info("[OrderFacade] Checkout and Pay 완료 - orderNumber: {}", orderNumber);
            return orderNumber;

        } catch (NoSuchElementException e) {
            log.error("[OrderFacade] 엔티티 조회 실패 - email: {}, error: {}", email, e.getMessage());
            throw new IllegalArgumentException("주문 처리 중 필요한 정보를 찾을 수 없습니다: " + e.getMessage());
        } catch (IllegalStateException e) {
            log.error("[OrderFacade] 업무 로직 오류 - email: {}, error: {}", email, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[OrderFacade] 예상치 못한 오류 - email: {}, error: {}", email, e.getMessage(), e);
            throw new RuntimeException("주문 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 취소 + 환불 처리
     * 
     * 처리 순서:
     * 1. 주문 조회 및 권한 확인
     * 2. 취소 가능 여부 확인
     * 3. 결제 취소
     * 4. 주문 상태 변경
     * 5. 로깅
     * 
     * @param ono 주문번호
     * @param email 요청자 이메일
     * @throws AccessDeniedException 권한 없음
     * @throws IllegalStateException 취소 불가능한 상태
     */
    public void cancelOrderWithRefund(Long ono, String email) {
        log.info("[OrderFacade] 주문 취소 및 환불 시작 - ono: {}, email: {}", ono, email);

        try {
            // 1. 주문 조회 및 권한 확인
            Orders order = validateOrderOwnership(ono, email);
            log.info("[OrderFacade] 주문 조회 및 권한 확인 완료");

            // 2. 취소 가능 여부 확인
            if (!order.canCancel()) {
                log.warn("[OrderFacade] 취소 불가능한 상태 - orderStatus: {}", order.getStatus());
                throw new IllegalStateException("취소할 수 없는 주문 상태입니다: " + order.getStatus());
            }

            // 3. 결제 취소 (PaymentFacade를 통해)
            if (order.getPayment() != null) {
                paymentService.cancelPayment(order.getOrderNumber(), "사용자 주문 취소");
                log.info("[OrderFacade] 결제 취소 완료 - paymentId: {}", order.getPayment().getPaymentId());
            }

            // 4. 주문 상태 변경
            order.changeStatus(OrderStatus.CANCELLED);
            log.info("[OrderFacade] 주문 상태 변경 완료 - newStatus: CANCELLED");

            log.info("[OrderFacade] 주문 취소 및 환불 완료 - ono: {}", ono);

        } catch (AccessDeniedException e) {
            log.warn("[OrderFacade] 권한 오류 - ono: {}, email: {}, error: {}", ono, email, e.getMessage());
            throw e;
        } catch (IllegalStateException e) {
            log.warn("[OrderFacade] 상태 오류 - ono: {}, error: {}", ono, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[OrderFacade] 취소 처리 중 오류 - ono: {}, email: {}, error: {}", 
                    ono, email, e.getMessage(), e);
            throw new RuntimeException("주문 취소 처리 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 주문 상세 정보 조회 (관련 정보 포함)
     * 
     * @param ono 주문번호
     * @param email 요청자 이메일
     * @return 주문 상세 정보
     */
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderDetail(Long ono, String email) {
        log.info("[OrderFacade] 주문 상세 조회 - ono: {}, email: {}", ono, email);

        try {
            // 1. 주문 조회 및 권한 확인
            Orders order = validateOrderOwnership(ono, email);

            // 2. DTO 변환 및 반환
            return convertToOrderResponseDTO(order);

        } catch (AccessDeniedException e) {
            log.warn("[OrderFacade] 조회 권한 없음 - ono: {}, email: {}", ono, email);
            throw e;
        } catch (Exception e) {
            log.error("[OrderFacade] 조회 중 오류 - ono: {}, email: {}, error: {}", 
                    ono, email, e.getMessage());
            throw new RuntimeException("주문 조회 중 오류가 발생했습니다");
        }
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    /**
     * 회원 검증
     */
    private Member validateMember(String email) {
        return memberRepository.findById(email)
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다: " + email));
    }

    /**
     * 주문 항목 검증 (상품 존재 여부, 재고 확인)
     */
    private void validateOrderItems(List<OrderItemDTO> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            throw new IllegalArgumentException("주문 상품이 없습니다");
        }

        for (OrderItemDTO item : orderItems) {
            Product product = productRepository.findById(item.getPno())
                    .orElseThrow(() -> new NoSuchElementException(
                            "상품을 찾을 수 없습니다: " + item.getPno()));

            if (product.getStock() < item.getQty()) {
                throw new IllegalStateException(
                        String.format("재고 부족: %s (요청: %d, 보유: %d)", 
                                product.getPname(), item.getQty(), product.getStock()));
            }
        }
    }

    /**
     * 총 금액 계산
     */
    private int calculateTotalAmount(List<OrderItemDTO> orderItems) {
        return orderItems.stream()
                .mapToInt(item -> item.getPrice() * item.getQty())
                .sum();
    }

    /**
     * 쿠폰 할인 적용 (미구현)
     */
    private int applyCouponDiscount(Long memberCouponId, String email, int totalAmount) {
        // TODO: 쿠폰 서비스 통합 필요
        return 0;
    }

    /**
     * 주문번호 생성
     * 형식: ORD + yyyyMMddHHmmss + 랜덤3자리
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "ORD" + timestamp + random;
    }

    /**
     * 주문 엔티티 생성
     */
    private Orders createOrderEntity(
            String orderNumber,
            Member member,
            List<OrderItemDTO> orderItemDTOs,
            int totalAmount,
            int discountAmount,
            int finalAmount) {

        Orders order = Orders.builder()
                .orderNumber(orderNumber)
                .member(member)
                .totalAmount(totalAmount)
                .discountAmount(discountAmount)
                .finalAmount(finalAmount)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        // 주문 항목 추가
        for (OrderItemDTO itemDTO : orderItemDTOs) {
            Product product = productRepository.findById(itemDTO.getPno())
                    .orElseThrow(() -> new NoSuchElementException(
                            "상품을 찾을 수 없습니다: " + itemDTO.getPno()));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .qty(itemDTO.getQty())
                    .price(itemDTO.getPrice())
                    .build();

            order.addOrderItem(orderItem);
        }

        return order;
    }

    /**
     * 배송 정보 등록
     */
    private void registerDelivery(Orders order, DeliveryDTO deliveryDTO) {
        if (deliveryDTO == null) {
            throw new IllegalArgumentException("배송 정보가 없습니다");
        }

        Delivery delivery = Delivery.builder()
                .receiverName(deliveryDTO.getReceiverName())
                .receiverPhone(deliveryDTO.getReceiverPhone())
                .address(deliveryDTO.getAddress())
                .zipCode(deliveryDTO.getZipCode())
                .deliveryMessage(deliveryDTO.getDeliveryMessage())
                .status(DeliveryStatus.READY)
                .build();

        order.setDelivery(delivery);
    }

    /**
     * 장바구니 비우기 (미구현)
     */
    private void clearCart(String email) {
        // TODO: 장바구니 서비스 통합 필요
    }

    /**
     * 주문 소유자 검증
     */
    private Orders validateOrderOwnership(Long ono, String email) {
        Orders order = orderRepository.findByIdWithDetails(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다: " + ono));

        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 접근 권한이 없습니다");
        }

        return order;
    }

    /**
     * 주문 응답 DTO 변환
     */
    private OrderResponseDTO convertToOrderResponseDTO(Orders order) {
        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .pno(item.getProduct().getPno())
                        .pname(item.getProduct().getPname())
                        .qty(item.getQty())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        DeliveryResponseDTO deliveryDTO = null;
        if (order.getDelivery() != null) {
            Delivery delivery = order.getDelivery();
            deliveryDTO = DeliveryResponseDTO.builder()
                    .dno(delivery.getDno())
                    .receiverName(delivery.getReceiverName())
                    .receiverPhone(delivery.getReceiverPhone())
                    .address(delivery.getAddress())
                    .zipCode(delivery.getZipCode())
                    .deliveryMessage(delivery.getDeliveryMessage())
                    .status(delivery.getStatus())
                    .trackingNumber(delivery.getTrackingNumber())
                    .build();
        }

        return OrderResponseDTO.builder()
                .ono(order.getOno())
                .orderNumber(order.getOrderNumber())
                .memberEmail(order.getMember().getEmail())
                .totalAmount(order.getTotalAmount())
                .discountAmount(order.getDiscountAmount())
                .finalAmount(order.getFinalAmount())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .orderItems(orderItemDTOs)
                .delivery(deliveryDTO)
                .build();
    }
}
