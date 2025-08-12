package com.cMall.feedShop.common.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 파일 업로드 결과 DTO
 */
@Builder
@Getter
public class UploadResult {
    private String originalFilename;
    private String storedFilename;
    private String filePath;
    private Long fileSize;
    private String contentType;
}