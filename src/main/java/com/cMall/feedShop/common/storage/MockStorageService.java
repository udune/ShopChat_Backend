package com.cMall.feedShop.common.storage;

import com.cMall.feedShop.common.dto.UploadResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Service
@Slf4j
@Profile("dev")
public class MockStorageService implements StorageService {

    @Value("${app.cdn.base-url}")
    private String cdnBaseUrl;

    @Override
        public List<UploadResult> uploadFilesWithDetails(List<MultipartFile> files, UploadDirectory directory) {
        log.info("📢 Mocking GCP Storage: 파일 업로드 로직 실행");

        // 리스트가 비어있거나 null인 경우 빈 리스트를 반환
        if (files == null || files.isEmpty()) {
            return Collections.emptyList();
        }

        // 실제 업로드 대신 가짜 결과를 반환합니다.
        String directoryPath = directory == UploadDirectory.REVIEWS ? "reviews" : "profiles";
        UploadResult mockResult = UploadResult.builder()
                .originalFilename("mock-file.jpg")
                .storedFilename("mock-" + files.get(0).getOriginalFilename())
                .filePath(cdnBaseUrl + "/images/" + directoryPath + "/" + files.get(0).getOriginalFilename())
                .fileSize(1000L)
                .contentType("image/jpeg")
                .build();

        return Collections.singletonList(mockResult);
    }

    @Override
    public boolean deleteFile(String filePath) {
        log.info("📢 Mocking GCP Storage: 파일 삭제 로직 실행 - {}", filePath);
        // 실제 삭제 로직 없이 항상 true를 반환
        return true;
    }
}