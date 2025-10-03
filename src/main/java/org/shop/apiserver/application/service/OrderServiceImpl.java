package org.shop.apiserver.application.service;

import org.shop.apiserver.application.dto.*;
import org.shop.apiserver.domain.model.delivery.Delivery;
import org.shop.apiserver.domain.model.delivery.DeliveryStatus;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.domain.model.order.OrderItem;
import org.shop.apiserver.domain.model.order.OrderStatus;
import org.shop.apiserver.domain.model.order.Orders;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.*;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final DeliveryRepository deliveryRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PaymentService paymentService;  // 추가

    @Override
    public String createOrder(OrderDTO orderDTO) {

        log.info("Creating order for: " + orderDTO.getEmail());

        // 1. 회원 정보 확인
        Member member = memberRepository.findById(orderDTO.getEmail())
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        // 2. 주문번호 생성
        String orderNumber = generateOrderNumber();

        // 3. 총 금액 계산
        int totalAmount = 0;
        for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
            totalAmount += itemDTO.getPrice() * itemDTO.getQty();
        }

        // 4. 주문 생성
        Orders order = Orders.builder()
                .orderNumber(orderNumber)
                .member(member)
                .totalAmount(totalAmount)
                .discountAmount(0)  // 쿠폰은 Phase 4에서 추가
                .finalAmount(totalAmount)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        // 5. 주문 아이템 추가
        for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
            Product product = productRepository.findById(itemDTO.getPno())
                    .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .qty(itemDTO.getQty())
                    .price(itemDTO.getPrice())
                    .build();

            order.addOrderItem(orderItem);
        }

        // 6. 배송 정보 추가
        DeliveryDTO deliveryDTO = orderDTO.getDelivery();
        Delivery delivery = Delivery.builder()
                .receiverName(deliveryDTO.getReceiverName())
                .receiverPhone(deliveryDTO.getReceiverPhone())
                .address(deliveryDTO.getAddress())
                .zipCode(deliveryDTO.getZipCode())
                .deliveryMessage(deliveryDTO.getDeliveryMessage())
                .status(DeliveryStatus.READY)
                .build();

        order.setDelivery(delivery);

        // 7. 주문 저장
        orderRepository.save(order);

        // 8. 결제 자동 처리 (추가)
        String paymentMethod = orderDTO.getPaymentMethod() != null ?
                orderDTO.getPaymentMethod() : "CARD";
        paymentService.processPayment(orderNumber, paymentMethod);

        // 9. 장바구니 비우기 (장바구니에서 주문한 경우)
        // cartItemRepository에서 해당 회원의 장바구니 아이템 삭제

        log.info("Order created and paid: " + orderNumber);

        return orderNumber;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long ono, String email) {

        Orders order = orderRepository.findByIdWithDetails(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        // 권한 체크 (본인의 주문만 조회 가능)
        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 조회 권한이 없습니다.");
        }

        return entityToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByOrderNumber(String orderNumber, String email) {

        Orders order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        // 권한 체크
        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 조회 권한이 없습니다.");
        }

        return entityToDTO(order);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponseDTO<OrderListDTO> getMyOrders(String email, PageRequestDTO pageRequestDTO) {

        Pageable pageable = PageRequest.of(
                pageRequestDTO.getPage() - 1,
                pageRequestDTO.getSize(),
                Sort.by("orderDate").descending()
        );

        Page<Orders> result = orderRepository.findByMemberEmail(email, pageable);

        List<OrderListDTO> dtoList = result.getContent().stream()
                .map(this::entityToListDTO)
                .collect(Collectors.toList());

        return PageResponseDTO.<OrderListDTO>withAll()
                .dtoList(dtoList)
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    public void cancelOrder(Long ono, String email) {

        Orders order = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        // 권한 체크
        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 취소 권한이 없습니다.");
        }

        // 취소 가능 상태 체크
        if (!order.canCancel()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다.");
        }

        // 결제 취소 (추가)
        if (order.getPayment() != null) {
            paymentService.cancelPayment(order.getOrderNumber(), "사용자 주문 취소");
        }

        // 재고 복구는 Phase 3에서 추가

        log.info("Order cancelled: " + order.getOrderNumber());
    }

    @Override
    public void updateOrderStatus(Long ono, String status) {

        Orders order = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        OrderStatus orderStatus = OrderStatus.valueOf(status);
        order.changeStatus(orderStatus);

        // 배송 상태도 함께 업데이트
        if (orderStatus == OrderStatus.SHIPPED && order.getDelivery() != null) {
            order.getDelivery().changeStatus(DeliveryStatus.SHIPPING);
        } else if (orderStatus == OrderStatus.DELIVERED && order.getDelivery() != null) {
            order.getDelivery().changeStatus(DeliveryStatus.COMPLETED);
        }

        log.info("Order status updated: " + order.getOrderNumber() + " -> " + status);
    }

    // ============================================
    // Private Helper Methods
    // ============================================

    /**
     * 주문번호 생성
     * 형식: ORD + yyyyMMddHHmmss + 랜덤3자리
     */
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "ORD" + timestamp + random;
    }

    /**
     * Entity -> DTO 변환
     */
    private OrderResponseDTO entityToDTO(Orders order) {

        // OrderItems 변환
        List<OrderItemDTO> orderItemDTOs = order.getOrderItems().stream()
                .map(item -> OrderItemDTO.builder()
                        .pno(item.getProduct().getPno())
                        .pname(item.getProduct().getPname())
                        .qty(item.getQty())
                        .price(item.getPrice())
                        .build())
                .collect(Collectors.toList());

        // Delivery 변환
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

    /**
     * Entity -> ListDTO 변환 (주문 목록용)
     */
    private OrderListDTO entityToListDTO(Orders order) {

        // 대표 상품명 가져오기
        String firstProductName = order.getOrderItems().isEmpty() ?
                "상품 없음" : order.getOrderItems().get(0).getProduct().getPname();

        // 상품이 2개 이상이면 "상품명 외 n개" 형식
        int productCount = order.getOrderItems().size();
        if (productCount > 1) {
            firstProductName += " 외 " + (productCount - 1) + "개";
        }

        return OrderListDTO.builder()
                .ono(order.getOno())
                .orderNumber(order.getOrderNumber())
                .finalAmount(order.getFinalAmount())
                .orderDate(order.getOrderDate())
                .status(order.getStatus())
                .firstProductName(firstProductName)
                .productCount(productCount)
                .build();
    }
}
