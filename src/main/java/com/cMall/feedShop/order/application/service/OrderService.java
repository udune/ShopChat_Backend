package com.cMall.feedShop.order.application.service;

import com.cMall.feedShop.cart.domain.model.CartItem;
import com.cMall.feedShop.cart.domain.repository.CartItemRepository;
import com.cMall.feedShop.common.exception.BusinessException;
import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.order.application.dto.request.OrderCreateRequest;
import com.cMall.feedShop.order.application.dto.response.OrderCreateResponse;
import com.cMall.feedShop.order.application.util.OrderCalculation;
import com.cMall.feedShop.order.domain.enums.OrderStatus;
import com.cMall.feedShop.order.domain.exception.OrderException;
import com.cMall.feedShop.order.domain.model.Order;
import com.cMall.feedShop.order.domain.model.OrderItem;
import com.cMall.feedShop.order.domain.repository.OrderRepository;
import com.cMall.feedShop.product.application.util.DiscountCalculator;
import com.cMall.feedShop.product.domain.exception.ProductException;
import com.cMall.feedShop.product.domain.model.Product;
import com.cMall.feedShop.product.domain.model.ProductImage;
import com.cMall.feedShop.product.domain.model.ProductOption;
import com.cMall.feedShop.product.domain.repository.ProductImageRepository;
import com.cMall.feedShop.product.domain.repository.ProductOptionRepository;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.exception.UserException;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.model.UserPoint;
import com.cMall.feedShop.user.domain.repository.UserPointRepository;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    final private UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductOptionRepository productOptionRepository;
    private final DiscountCalculator discountCalculator;
    private final OrderRepository orderRepository;
    private final UserPointRepository userPointRepository;
    private final ProductImageRepository productImageRepository;

    /**
     * 주문 생성
     * @param request 주문 생성 요청 정보
     * @param userDetails 현재 로그인된 사용자 정보
     * @return 주문 생성 응답 정보
     */
    public OrderCreateResponse createOrder(OrderCreateRequest request, UserDetails userDetails) {
        // 1. 현재 사용자 조회
        User currentUser = getCurrentUser(userDetails);

        // 2. 사용자 권한 검증
        validateUserPermission(currentUser);

        // 3. 선택된 장바구니 아이템 조회 (selected = true 인 아이템들만)
        List<CartItem> selectedCartItems = getSelectedCartItems(currentUser.getId());
        if (selectedCartItems.isEmpty()) {
            throw new OrderException(ErrorCode.ORDER_CART_EMPTY);
        }

        // 4. 상품 옵션 조회 및 재고 확인
        Map<Long, ProductOption> optionMap = validateGetProductOptions(selectedCartItems);

        // 5. 상품 이미지 조회
        Map<Long, ProductImage> imageMap = validateGetProductImages(selectedCartItems);

        // 6. 주문 금액 계산
        OrderCalculation calculation = calculateOrderAmount(selectedCartItems, optionMap, request.getUsedPoints());

        // 7. 포인트 사용 가능 여부 확인
        validatePointUsage(currentUser, calculation.getActualUsedPoints());

        // 8. 주문 생성
        Order order = createOrderEntity(currentUser, request, calculation);

        // 9. 주문 아이템 생성
        createOrderItems(order, selectedCartItems, optionMap, imageMap);

        // 10. 재고 차감
        decreaseStock(selectedCartItems, optionMap);

        // 11. 주문 저장
        Order savedOrder = orderRepository.save(order);

        // 12. 포인트 처리 (사용 및 적립)
        processUserPoints(currentUser, calculation.getActualUsedPoints(), calculation.getEarnedPoints());

        // 13. 장바구니 아이템 삭제 (선택된 아이템들만)
        cartItemRepository.deleteAll(selectedCartItems);

        // 13. 주문 생성 응답 반환
        return OrderCreateResponse.from(savedOrder);
    }

    // JWT 에서 현재 사용자 정보 조회
    private User getCurrentUser(UserDetails userDetails) {
        String login_id = userDetails.getUsername();
        return userRepository.findByLoginId(login_id)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
    }

    // 사용자 권한 검증
    private void validateUserPermission(User user) {
        if (user.getRole() != UserRole.USER) {
            throw new OrderException(ErrorCode.ORDER_FORBIDDEN);
        }
    }

    // 선택된 장바구니 아이템 조회 (selected = true 인 아이템들만)
    private List<CartItem> getSelectedCartItems(Long userId) {
        List<CartItem> cartItems = cartItemRepository.findByUserIdWithCart(userId);
        return cartItems.stream()
                .filter(CartItem::getSelected)
                .toList();
    }

    private Map<Long, ProductImage> validateGetProductImages(List<CartItem> cartItems) {
        // 장바구니 아이템에서 이미지 ID를 추출하여 중복 제거
        Set<Long> imageIds = cartItems.stream()
                .map(CartItem::getImageId)
                .collect(Collectors.toSet());

        return productImageRepository.findAllById(imageIds).stream()
                .collect(Collectors.toMap(ProductImage::getImageId, Function.identity()));
    }

    // 상품 옵션 조회 및 재고 확인
    private Map<Long, ProductOption> validateGetProductOptions(List<CartItem> cartItems) {
        // 장바구니 아이템에서 옵션 ID를 추출하여 중복 제거
        Set<Long> optionIds = cartItems.stream()
                .map(CartItem::getOptionId)
                .collect(Collectors.toSet());

        // 옵션 ID가 비어있으면 예외 처리
        List<ProductOption> options = productOptionRepository.findAllByOptionIdIn(optionIds);
        if (options.size() != optionIds.size()) {
            throw new ProductException(ErrorCode.PRODUCT_OPTION_NOT_FOUND);
        }

        // 재고를 확인한다.
        Map<Long, ProductOption> optionMap = options.stream()
                .collect(Collectors.toMap(ProductOption::getOptionId, Function.identity()));

        for (CartItem cartItem : cartItems) {
            ProductOption option = optionMap.get(cartItem.getOptionId());
            if (!option.isInStock() || option.getStock() < cartItem.getQuantity()) {
                throw new ProductException(ErrorCode.OUT_OF_STOCK);
            }
        }

        return optionMap;
    }

    // 주문 금액 계산
    // 포인트 주문 금액의 최대 10%까지만 사용 가능
    // 포인트 차감 (100 포인트 = 100 원)
    // 적립 포인트 (총 구매금액 1만원 당 50점)
    private OrderCalculation calculateOrderAmount(List<CartItem> cartItems, Map<Long, ProductOption> optionMap, Integer usedPoints) {
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (CartItem cartItem : cartItems) {
            // 장바구니 아이템에서 옵션 ID로 상품을 조회한다.
            ProductOption option = optionMap.get(cartItem.getOptionId());
            Product product = option.getProduct();

            // 상품의 할인된 가격을 계산해서 알아낸다.
            BigDecimal discountPrice = discountCalculator.calculateDiscountPrice(
                    product.getPrice(),
                    product.getDiscountType(),
                    product.getDiscountValue()
            );

            // 할인된 가격에 장바구니 아이템의 수량을 곱한다.
            BigDecimal itemTotal = discountPrice.multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            // totalAmount에 수량이 곱해진 할인된 가격을 더한다.
            totalAmount = totalAmount.add(itemTotal);
        }

        // totalAmount * 0.1을 계산하여 최대 포인트 사용량을 계산한다. (주문 금액의 최대 10%까지만 사용 가능)
        BigDecimal maxPointUsage = totalAmount.multiply(BigDecimal.valueOf(0.1))
                .setScale(0, BigDecimal.ROUND_DOWN);

        // 사용자가 요청한 포인트 사용량이 최대 포인트 사용량을 초과하는지 확인한다.
        Integer actualUsedPoints = 0;
        if (usedPoints != null && usedPoints > 0)  {
            BigDecimal requestedPointAmount = BigDecimal.valueOf(usedPoints);
            if (requestedPointAmount.compareTo(maxPointUsage) <= 0) {
                actualUsedPoints = usedPoints;
            } else {
                actualUsedPoints = maxPointUsage.intValue();
            }
        }

        // 포인트를 totalAmount 에서 차감한다. (100 포인트 = 100 원)
        BigDecimal pointDeduction = BigDecimal.valueOf(actualUsedPoints);
        BigDecimal finalAmount = totalAmount.subtract(pointDeduction);

        // 최종 금액이 0보다 작으면 0으로 설정한다.
        if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            finalAmount = BigDecimal.ZERO;
        }

        // 최종 금액을 기준으로 포인트를 계산한다. (총 구매금액 1만원 당 50점)
        Integer earnedPoints = calculatePurchasePoints(finalAmount);

        // OrderCalculation 객체를 생성하여 반환한다.
        return OrderCalculation.builder()
                .totalAmount(totalAmount)
                .finalAmount(finalAmount)
                .actualUsedPoints(actualUsedPoints)
                .earnedPoints(earnedPoints)
                .build();
    }

    // 구매 후 얻을 포인트를 계산한다.
    private Integer calculatePurchasePoints(BigDecimal finalAmount) {
        if (finalAmount == null || finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        // 최종 금액을 10,000으로 나눈다.
        BigDecimal tenThousand = BigDecimal.valueOf(10000);
        BigDecimal units = finalAmount.divide(tenThousand, 0, BigDecimal.ROUND_DOWN);

        // 10,000원 단위로 50 포인트를 적립한다.
        return units.multiply(BigDecimal.valueOf(50)).intValue();
    }

    // 포인트가 유효한지 검증한다.
    private void validatePointUsage(User user, Integer usedPoints) {
        // 사용할 포인트 검증
        if (usedPoints == null || usedPoints == 0) {
            return;
        }

        // 유효값 및 100 포인트 단위 검증 (100 포인트 단위여야 한다)
        if (usedPoints < 0 || usedPoints % 100 != 0) {
            throw new OrderException(ErrorCode.INVALID_POINT);
        }

        // 사용자 포인트 검증
        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElse(UserPoint.builder()
                        .user(user)
                        .currentPoints(0)
                        .build());
        if (!userPoint.canUsePoints(usedPoints)) {
            throw new OrderException(ErrorCode.OUT_OF_POINT);
        }
    }

    // 주문 Entity 생성
    private Order createOrderEntity(User user, OrderCreateRequest request, OrderCalculation calculation) {
        return Order.builder()
                .user(user)
                .status(OrderStatus.ORDERED)
                .totalPrice(calculation.getTotalAmount())
                .finalPrice(calculation.getFinalAmount())
                .deliveryFee(request.getDeliveryFee())
                .usedPoints(calculation.getActualUsedPoints())
                .earnedPoints(calculation.getEarnedPoints())
                .deliveryAddress(request.getDeliveryAddress())
                .deliveryDetailAddress(request.getDeliveryDetailAddress())
                .postalCode(request.getPostalCode())
                .recipientName(request.getRecipientName())
                .recipientPhone(request.getRecipientPhone())
                .deliveryMessage(request.getDeliveryMessage())
                .paymentMethod(request.getPaymentMethod())
                .cardNumber(request.getCardNumber())
                .cardExpiry(request.getCardExpiry())
                .cardCvc(request.getCardCvc())
                .build();
    }

    // 장바구니 아이템을 주문의 주문 아이템 Entity로 만든다.
    private void createOrderItems(Order order, List<CartItem> cartItems, Map<Long, ProductOption> optionMap, Map<Long, ProductImage> imageMap) {
        for (CartItem cartItem : cartItems) {
            ProductOption option = optionMap.get(cartItem.getOptionId());
            ProductImage image = imageMap.get(cartItem.getImageId());
            Product product = option.getProduct();

            // totalPrice : 상품의 원래 가격
            BigDecimal totalPrice = product.getPrice();
            // finalPrice : 상품의 할인된 최종 가격
            BigDecimal finalPrice = discountCalculator.calculateDiscountPrice(
                    totalPrice,
                    product.getDiscountType(),
                    product.getDiscountValue()
            );

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productOption(option)
                    .productImage(image)
                    .quantity(cartItem.getQuantity())
                    .totalPrice(totalPrice)
                    .finalPrice(finalPrice)
                    .build();

            order.addOrderItem(orderItem);
        }
    }

    // 재고 차감
    private void decreaseStock(List<CartItem> cartItems, Map<Long, ProductOption> optionMap) {
        // 모든 장바구니 아이템들의 option을 조회한다.
        // option의 stock 에서 장바구니 아이템의 quantity를 뺀다.
        for (CartItem cartItem : cartItems) {
            ProductOption option = optionMap.get(cartItem.getOptionId());
            option.decreaseStock(cartItem.getQuantity());
            productOptionRepository.save(option);
        }
    }

    // UserPoint에 적립 및 사용
    private void processUserPoints(User user, Integer usedPoints, Integer earnedPoints) {
        // UserPoint를 조회한다.
        UserPoint userPoint = userPointRepository.findByUser(user)
                .orElse(UserPoint.builder()
                        .user(user)
                        .currentPoints(0)
                        .build());

        // 포인트 사용
        if (usedPoints != null && usedPoints > 0) {
            userPoint.usePoints(usedPoints);
        }

        // 포인트 적립
        if (earnedPoints != null && earnedPoints > 0) {
            userPoint.earnPoints(earnedPoints);
        }

        // DB에 저장
        userPointRepository.save(userPoint);
    }
}
