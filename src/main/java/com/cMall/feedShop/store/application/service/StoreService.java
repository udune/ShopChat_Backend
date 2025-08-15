package com.cMall.feedShop.store.application.service;

import com.cMall.feedShop.store.application.dto.response.StoreListResponse;
import com.cMall.feedShop.store.domain.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;

    public List<StoreListResponse> getAllStores() {
        List<StoreListResponse> stores = storeRepository.findAllStoresOrderByName().stream()
                .map(StoreListResponse::from)
                .toList();

        return stores;
    }

}
