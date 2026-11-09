package com.daesabu.meongcoach.home.application;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 홈 바로 훈련 카드에서 강아지 나이에 따라 앞쪽 카테고리를 숨기는 기준이다. 코드 배포 없이 환경 변수로 바꾼다.
 *
 * @param months 강아지가 이 개월 수 이상이면 minCategoryOrder 기준을 적용한다
 * @param minCategoryOrder 그때 보여 줄 카테고리 정렬 값(sort_order)의 최솟값. prod 기준 3은 주니어 교육이다
 */
@Validated
@ConfigurationProperties("meongcoach.home.quick-topic")
public record QuickTopicProperties(@Positive int months, @Positive int minCategoryOrder) {
}
