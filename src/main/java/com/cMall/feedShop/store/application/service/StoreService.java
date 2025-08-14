package com.cMall.feedShop.store.application.service;

import com.cMall.feedShop.common.exception.ErrorCode;
import com.cMall.feedShop.store.application.dto.response.StoreDetailResponse;
import com.cMall.feedShop.store.domain.exception.StoreException;
import com.cMall.feedShop.store.domain.model.Store;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import com.cMall.feedShop.user.domain.model.User;
import com.cMall.feedShop.user.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;

    /**
     * 판매자의 가게 상세 정보를 조회합니다.
     *
     * @param loginId 판매자의 로그인 ID
     * @return 판매자의 가게 상세 정보
     */
    public StoreDetailResponse getMyStoreDetail(String loginId) {
        User currentUser = getCurrentUser(loginId);
        Store store = getUserStore(currentUser.getId());

        return StoreDetailResponse.from(store);
    }

    // 사용자 검증
    private User getCurrentUser(String loginId) {
        return userRepository.findByLoginId(loginId)
                .orElseThrow(() -> new StoreException(ErrorCode.USER_NOT_FOUND));
    }

    // 판매자의 가게 조회
    private Store getUserStore(Long sellerId) {
        return storeRepository.findBySellerId(sellerId)
                .orElseThrow(() -> new StoreException(ErrorCode.STORE_NOT_FOUND, "판매자의 가게를 찾을 수 없습니다."));
    }
}
