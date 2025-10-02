package org.shop.apiserver.service;

import jakarta.transaction.Transactional;
import org.shop.apiserver.dto.*;

@Transactional
public interface OrderService {

    /**
     * 주문 생성
     * @param orderDTO 주문 정보
     * @return 생성된 주문 번호
     */
    String createOrder(OrderDTO orderDTO);

    /**
     * 주문 상세 조회
     * @param ono 주문 ID
     * @param email 회원 이메일 (권한 체크용)
     * @return 주문 상세 정보
     */
    OrderResponseDTO getOrder(Long ono, String email);

    /**
     * 주문번호로 조회
     * @param orderNumber 주문번호
     * @param email 회원 이메일
     * @return 주문 상세 정보
     */
    OrderResponseDTO getOrderByOrderNumber(String orderNumber, String email);

    /**
     * 내 주문 목록 조회
     * @param email 회원 이메일
     * @param pageRequestDTO 페이징 정보
     * @return 주문 목록
     */
    PageResponseDTO<OrderListDTO> getMyOrders(String email, PageRequestDTO pageRequestDTO);

    /**
     * 주문 취소
     * @param ono 주문 ID
     * @param email 회원 이메일
     */
    void cancelOrder(Long ono, String email);

    /**
     * 주문 상태 변경 (관리자용)
     * @param ono 주문 ID
     * @param status 변경할 상태
     */
    void updateOrderStatus(Long ono, String status);
}
