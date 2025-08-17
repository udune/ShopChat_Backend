package com.cMall.feedShop.user.domain.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RewardType {
    
    // 관리자 발급
    ADMIN_GRANT("관리자 지급", "관리자가 직접 지급한 포인트"),
    
    // 이벤트 참여
    EVENT_PARTICIPATION("이벤트 참여", "이벤트 참여 보상"),
    EVENT_WINNER("이벤트 당첨", "이벤트 당첨 보상"),
    
    // 리뷰 관련
    REVIEW_WRITE("리뷰 작성", "리뷰 작성 보상"),
    REVIEW_PHOTO("리뷰 사진", "리뷰 사진 첨부 보상"),
    REVIEW_QUALITY("리뷰 품질", "고품질 리뷰 보상"),
    
    // 생일 축하
    BIRTHDAY("생일 축하", "생일 축하 포인트"),
    
    // 첫 구매
    FIRST_PURCHASE("첫 구매", "첫 구매 보너스"),
    
    // 추천인
    REFERRAL("추천인", "친구 추천 보상"),
    REFERRED("피추천인", "추천받은 사용자 보상"),
    
    // 기타
    COMPENSATION("보상", "기타 보상"),
    ADJUSTMENT("조정", "포인트 조정");
    
    private final String displayName;
    private final String description;
}
