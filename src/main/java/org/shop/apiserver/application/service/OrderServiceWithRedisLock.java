package org.shop.apiserver.application.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.shop.apiserver.application.dto.*;
import org.shop.apiserver.domain.model.delivery.Delivery;
import org.shop.apiserver.domain.model.delivery.DeliveryStatus;
import org.shop.apiserver.domain.model.member.Member;
import org.shop.apiserver.domain.model.order.OrderItem;
import org.shop.apiserver.domain.model.order.OrderStatus;
import org.shop.apiserver.domain.model.order.Orders;
import org.shop.apiserver.domain.model.product.Product;
import org.shop.apiserver.infrastructure.persistence.jpa.MemberRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.OrderRepository;
import org.shop.apiserver.infrastructure.persistence.jpa.ProductRepository;
import org.springframework.context.annotation.Primary;
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

@Service("redisLockOrderService")  // ✅ Bean 이름 지정
@Primary  // ✅ 기본으로 이 구현체 사용
@Transactional
@Log4j2
@RequiredArgsConstructor
public class OrderServiceWithRedisLock implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final PaymentService paymentService;
    private final CouponService couponService;
    private final RedisLockService redisLockService;  // ✅ Redis Lock 추가

    @Override
    public String createOrder(OrderDTO orderDTO) {
        log.info("🔓 Creating order with Redis Distributed Lock");

        Member member = memberRepository.findById(orderDTO.getEmail())
                .orElseThrow(() -> new NoSuchElementException("회원을 찾을 수 없습니다."));

        String orderNumber = generateOrderNumber();
        Orders order = Orders.builder()
                .orderNumber(orderNumber)
                .member(member)
                .orderDate(LocalDateTime.now())
                .status(OrderStatus.PENDING)
                .build();

        int totalAmount = 0;

        // ✅ Redis 분산락 사용
        for (OrderItemDTO itemDTO : orderDTO.getOrderItems()) {
            String lockKey = "product:lock:" + itemDTO.getPno();

            Product product = redisLockService.executeWithLock(
                    lockKey,
                    5,
                    10,
                    () -> {
                        Product p = productRepository.findById(itemDTO.getPno())
                                .orElseThrow(() -> new NoSuchElementException("상품을 찾을 수 없습니다."));

                        p.decreaseStock(itemDTO.getQty());
                        return productRepository.save(p);
                    }
            );

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .qty(itemDTO.getQty())
                    .price(product.getPrice())
                    .build();

            order.addOrderItem(orderItem);
            totalAmount += product.getPrice() * itemDTO.getQty();
        }

        // ... 나머지 로직 동일

        int discountAmount = 0;
        if (orderDTO.getMemberCouponId() != null) {
            try {
                discountAmount = couponService.useCoupon(
                        orderDTO.getMemberCouponId(),
                        orderDTO.getEmail(),
                        totalAmount
                );
            } catch (Exception e) {
                log.error("Coupon use failed: {}", e.getMessage());
            }
        }

        int finalAmount = totalAmount - discountAmount;
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount);
        order.setFinalAmount(finalAmount);

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
        orderRepository.save(order);

        String paymentMethod = orderDTO.getPaymentMethod() != null ?
                orderDTO.getPaymentMethod() : "CARD";
        paymentService.processPayment(orderNumber, paymentMethod);

        log.info("Order created with Redis lock: {}", orderNumber);
        return orderNumber;
    }

    @Override
    public void cancelOrder(Long ono, String email) {
        Orders order = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 취소 권한이 없습니다.");
        }

        if (!order.canCancel()) {
            throw new IllegalStateException("취소할 수 없는 주문 상태입니다.");
        }

        // ✅ Redis 분산락으로 재고 복구
        for (OrderItem item : order.getOrderItems()) {
            String lockKey = "product:lock:" + item.getProduct().getPno();

            redisLockService.executeWithLock(lockKey, 5, 10, () -> {
                Product product = productRepository.findById(item.getProduct().getPno())
                        .orElseThrow();
                product.increaseStock(item.getQty());
                productRepository.save(product);
            });
        }

        if (order.getPayment() != null) {
            paymentService.cancelPayment(order.getOrderNumber(), "사용자 주문 취소");
        }

        order.changeStatus(OrderStatus.CANCELLED);
        log.info("Order cancelled with Redis lock: {}", order.getOrderNumber());
    }

    // 나머지 메서드들 구현...
    private String generateOrderNumber() {
        String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = (int) (Math.random() * 900) + 100;
        return "ORD" + timestamp + random;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public OrderResponseDTO getOrder(Long ono, String email) {
        Orders order = orderRepository.findByIdWithDetails(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 조회 권한이 없습니다.");
        }

        return entityToDTO(order);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public OrderResponseDTO getOrderByOrderNumber(String orderNumber, String email) {
        Orders order = orderRepository.findByOrderNumberWithDetails(orderNumber)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        if (!order.getMember().getEmail().equals(email)) {
            throw new AccessDeniedException("주문 조회 권한이 없습니다.");
        }

        return entityToDTO(order);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
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
    public void updateOrderStatus(Long ono, String status) {
        Orders order = orderRepository.findById(ono)
                .orElseThrow(() -> new NoSuchElementException("주문을 찾을 수 없습니다."));

        OrderStatus orderStatus = OrderStatus.valueOf(status);
        order.changeStatus(orderStatus);

        if (orderStatus == OrderStatus.SHIPPED && order.getDelivery() != null) {
            order.getDelivery().changeStatus(DeliveryStatus.SHIPPING);
        } else if (orderStatus == OrderStatus.DELIVERED && order.getDelivery() != null) {
            order.getDelivery().changeStatus(DeliveryStatus.COMPLETED);
        }

        log.info("Order status updated: {} -> {}", order.getOrderNumber(), status);
    }

    private OrderResponseDTO entityToDTO(Orders order) {
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

    private OrderListDTO entityToListDTO(Orders order) {
        String firstProductName = order.getOrderItems().isEmpty() ?
                "상품 없음" : order.getOrderItems().get(0).getProduct().getPname();

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