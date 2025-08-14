package com.cMall.feedShop.store.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.store.application.dto.response.StoreDetailResponse;
import com.cMall.feedShop.store.domain.exception.StoreException;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import com.cMall.feedShop.user.domain.enums.UserRole;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * StoreService 단위 테스트
 *
 * 테스트 목표:
 * 1. 정상적인 가게 상세 조회 성공
 * 2. 사용자 없음 예외 처리
 * 3. 가게 없음 예외 처리
 * 4. 응답 데이터 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 단위 테스트")
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    // 테스트 데이터
    private User seller;
    private User buyer;
    private Store store;
    private final String LOGIN_ID = "seller123";
    private final Long USER_ID = 1L;
    private final Long STORE_ID = 1L;

    @BeforeEach
    void setUp() {
        setupTestData();
    }

    /**
     * 테스트 데이터 초기화
     */
    private void setupTestData() {
        // 판매자 사용자 생성
        seller = new User(LOGIN_ID, "password123", "seller@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(seller, "id", USER_ID);

        // 일반 사용자 생성
        buyer = new User("buyer123", "password123", "buyer@test.com", UserRole.USER);
        ReflectionTestUtils.setField(buyer, "id", 2L);

        // 가게 생성
        store = Store.builder()
                .storeName("테스트 스토어")
                .description("테스트 스토어입니다.")
                .logo("https://test-logo-url.com")
                .sellerId(USER_ID)
                .build();
        ReflectionTestUtils.setField(store, "storeId", STORE_ID);

        // 생성/수정 시간 설정
        LocalDateTime now = LocalDateTime.of(2025, 7, 10, 0, 0, 0);
        ReflectionTestUtils.setField(store, "createdAt", now);
        ReflectionTestUtils.setField(store, "updatedAt", now);
    }

    @Test
    @DisplayName("가게 상세 조회 성공")
    void getMyStoreDetail_Success() {
        // given
        given(userRepository.findByLoginId(LOGIN_ID))
                .willReturn(Optional.of(seller));
        given(storeRepository.findBySellerId(USER_ID))
                .willReturn(Optional.of(store));

        // when
        StoreDetailResponse result = storeService.getMyStoreDetail(LOGIN_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getStoreName()).isEqualTo("테스트 스토어");
        assertThat(result.getDescription()).isEqualTo("테스트 스토어입니다.");
        assertThat(result.getLogo()).isEqualTo("https://test-logo-url.com");
        assertThat(result.getSellerId()).isEqualTo(USER_ID);
        assertThat(result.getCreatedAt()).isEqualTo(LocalDateTime.of(2025, 7, 10, 0, 0, 0));
        assertThat(result.getUpdatedAt()).isEqualTo(LocalDateTime.of(2025, 7, 10, 0, 0, 0));

        // 메서드 호출 검증
        verify(userRepository, times(1)).findByLoginId(LOGIN_ID);
        verify(storeRepository, times(1)).findBySellerId(USER_ID);
    }

    @Test
    @DisplayName("사용자를 찾을 수 없음 - StoreException 발생")
    void getMyStoreDetail_UserNotFound() {
        // given
        given(userRepository.findByLoginId(LOGIN_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(LOGIN_ID))
                .isInstanceOf(StoreException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

        // 메서드 호출 검증
        verify(userRepository, times(1)).findByLoginId(LOGIN_ID);
        verify(storeRepository, times(0)).findBySellerId(USER_ID);
    }

    @Test
    @DisplayName("가게를 찾을 수 없음 - StoreException 발생")
    void getMyStoreDetail_StoreNotFound() {
        // given
        given(userRepository.findByLoginId(LOGIN_ID))
                .willReturn(Optional.of(seller));
        given(storeRepository.findBySellerId(USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(LOGIN_ID))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("판매자의 가게를 찾을 수 없습니다.");

        // 메서드 호출 검증
        verify(userRepository, times(1)).findByLoginId(LOGIN_ID);
        verify(storeRepository, times(1)).findBySellerId(USER_ID);
    }

    @Test
    @DisplayName("빈 문자열 로그인 ID로 조회 시도")
    void getMyStoreDetail_EmptyLoginId() {
        // given
        String emptyLoginId = "";
        given(userRepository.findByLoginId(emptyLoginId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(emptyLoginId))
                .isInstanceOf(StoreException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

        verify(userRepository, times(1)).findByLoginId(emptyLoginId);
    }

    @Test
    @DisplayName("존재하지 않는 로그인 ID로 조회 시도")
    void getMyStoreDetail_NonExistentLoginId() {
        // given
        String nonExistentLoginId = "nonexistent123";
        given(userRepository.findByLoginId(nonExistentLoginId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(nonExistentLoginId))
                .isInstanceOf(StoreException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

        verify(userRepository, times(1)).findByLoginId(nonExistentLoginId);
    }

    @Test
    @DisplayName("사용자는 존재하지만 가게가 없는 경우")
    void getMyStoreDetail_UserExistsButNoStore() {
        // given
        given(userRepository.findByLoginId(LOGIN_ID))
                .willReturn(Optional.of(seller));
        given(storeRepository.findBySellerId(USER_ID))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(LOGIN_ID))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("판매자의 가게를 찾을 수 없습니다.");

        verify(userRepository, times(1)).findByLoginId(LOGIN_ID);
        verify(storeRepository, times(1)).findBySellerId(USER_ID);
    }

    @Test
    @DisplayName("일반 사용자(buyer)의 가게 조회 시도")
    void getMyStoreDetail_BuyerUser() {
        // given
        String buyerLoginId = "buyer123";
        Long buyerId = 2L;

        given(userRepository.findByLoginId(buyerLoginId))
                .willReturn(Optional.of(buyer));
        given(storeRepository.findBySellerId(buyerId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail(buyerLoginId))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("판매자의 가게를 찾을 수 없습니다.");

        verify(userRepository, times(1)).findByLoginId(buyerLoginId);
        verify(storeRepository, times(1)).findBySellerId(buyerId);
    }

    @Test
    @DisplayName("가게 정보가 null인 경우 처리")
    void getMyStoreDetail_StoreWithNullFields() {
        // given
        Store storeWithNulls = Store.builder()
                .storeName("테스트 스토어")
                .description(null) // null 설명
                .logo(null) // null 로고
                .sellerId(USER_ID)
                .build();
        ReflectionTestUtils.setField(storeWithNulls, "storeId", STORE_ID);

        given(userRepository.findByLoginId(LOGIN_ID))
                .willReturn(Optional.of(seller));
        given(storeRepository.findBySellerId(USER_ID))
                .willReturn(Optional.of(storeWithNulls));

        // when
        StoreDetailResponse result = storeService.getMyStoreDetail(LOGIN_ID);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getStoreId()).isEqualTo(STORE_ID);
        assertThat(result.getStoreName()).isEqualTo("테스트 스토어");
        assertThat(result.getDescription()).isNull();
        assertThat(result.getLogo()).isNull();
        assertThat(result.getSellerId()).isEqualTo(USER_ID);
    }
}

/**
 * StoreService 통합 테스트 시나리오
 * 실제 비즈니스 로직의 흐름을 검증하는 시나리오 기반 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 통합 테스트 시나리오")
class StoreServiceIntegrationTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    @DisplayName("시나리오 1: 신규 판매자가 가게를 등록한 후 조회")
    void scenario1_NewSellerStoreRegistrationAndRetrieval() {
        // given - 신규 판매자 등록
        User newSeller = new User("newseller", "password", "new@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(newSeller, "id", 100L);

        Store newStore = Store.builder()
                .storeName("신규 가게")
                .description("새로 만든 가게입니다")
                .logo("https://new-store-logo.com")
                .sellerId(100L)
                .build();
        ReflectionTestUtils.setField(newStore, "storeId", 100L);

        given(userRepository.findByLoginId("newseller"))
                .willReturn(Optional.of(newSeller));
        given(storeRepository.findBySellerId(100L))
                .willReturn(Optional.of(newStore));

        // when
        StoreDetailResponse result = storeService.getMyStoreDetail("newseller");

        // then
        assertThat(result.getStoreName()).isEqualTo("신규 가게");
        assertThat(result.getDescription()).isEqualTo("새로 만든 가게입니다");
        assertThat(result.getSellerId()).isEqualTo(100L);

        System.out.println("✅ 시나리오 1 완료: 신규 판매자 가게 조회 성공");
    }

    @Test
    @DisplayName("시나리오 2: 기존 판매자가 가게 정보를 수정한 후 조회")
    void scenario2_ExistingSellerStoreUpdateAndRetrieval() {
        // given - 기존 판매자의 가게 정보 수정
        User existingSeller = new User("existing", "password", "existing@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(existingSeller, "id", 200L);

        Store updatedStore = Store.builder()
                .storeName("업데이트된 가게명")
                .description("새로운 설명입니다")
                .logo("https://updated-logo.com")
                .sellerId(200L)
                .build();
        ReflectionTestUtils.setField(updatedStore, "storeId", 200L);

        given(userRepository.findByLoginId("existing"))
                .willReturn(Optional.of(existingSeller));
        given(storeRepository.findBySellerId(200L))
                .willReturn(Optional.of(updatedStore));

        // when
        StoreDetailResponse result = storeService.getMyStoreDetail("existing");

        // then
        assertThat(result.getStoreName()).isEqualTo("업데이트된 가게명");
        assertThat(result.getDescription()).isEqualTo("새로운 설명입니다");
        assertThat(result.getLogo()).isEqualTo("https://updated-logo.com");

        System.out.println("✅ 시나리오 2 완료: 기존 판매자 가게 정보 수정 후 조회 성공");
    }

    @Test
    @DisplayName("시나리오 3: 판매자 계정 삭제 후 가게 조회 시도")
    void scenario3_DeletedSellerStoreRetrievalAttempt() {
        // given - 삭제된 판매자 계정
        given(userRepository.findByLoginId("deleted_seller"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail("deleted_seller"))
                .isInstanceOf(StoreException.class)
                .hasMessage(ErrorCode.USER_NOT_FOUND.getMessage());

        System.out.println("✅ 시나리오 3 완료: 삭제된 판매자 계정 접근 차단 검증");
    }

    @Test
    @DisplayName("시나리오 4: 판매자가 가게를 폐업한 후 조회 시도")
    void scenario4_ClosedStoreRetrievalAttempt() {
        // given - 가게를 폐업한 판매자
        User sellerWithClosedStore = new User("closed_store_seller", "password", "closed@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(sellerWithClosedStore, "id", 300L);

        given(userRepository.findByLoginId("closed_store_seller"))
                .willReturn(Optional.of(sellerWithClosedStore));
        given(storeRepository.findBySellerId(300L))
                .willReturn(Optional.empty()); // 폐업으로 인한 가게 없음

        // when & then
        assertThatThrownBy(() -> storeService.getMyStoreDetail("closed_store_seller"))
                .isInstanceOf(StoreException.class)
                .hasMessageContaining("판매자의 가게를 찾을 수 없습니다.");

        System.out.println("✅ 시나리오 4 완료: 폐업한 가게 조회 차단 검증");
    }
}

/**
 * StoreService 성능 테스트
 * 대용량 데이터나 반복 호출에 대한 성능을 검증
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("StoreService 성능 테스트")
class StoreServicePerformanceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private StoreService storeService;

    @Test
    @DisplayName("반복 조회 성능 테스트")
    void performanceTest_RepeatedCalls() {
        // given
        User seller = new User("seller", "password", "seller@test.com", UserRole.SELLER);
        ReflectionTestUtils.setField(seller, "id", 1L);

        Store store = Store.builder()
                .storeName("테스트 스토어")
                .description("성능 테스트용 스토어")
                .sellerId(1L)
                .build();
        ReflectionTestUtils.setField(store, "storeId", 1L);

        given(userRepository.findByLoginId("seller"))
                .willReturn(Optional.of(seller));
        given(storeRepository.findBySellerId(1L))
                .willReturn(Optional.of(store));

        // when - 1000번 반복 호출
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            StoreDetailResponse result = storeService.getMyStoreDetail("seller");
            assertThat(result).isNotNull();
        }

        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // then
        System.out.println("1000번 호출 실행 시간: " + executionTime + "ms");
        assertThat(executionTime).isLessThan(5000); // 5초 이내 완료 검증

        // Mock 호출 횟수 검증
        verify(userRepository, times(1000)).findByLoginId("seller");
        verify(storeRepository, times(1000)).findBySellerId(1L);
    }
}