package com.daesabu.meongcoach.home.application;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 홈 바로 훈련 카드에서 나이 든 강아지에게 앞쪽 카테고리를 숨기는 기준이다. 코드 배포 없이 환경 변수로 바꾼다.
 *
 * @param olderDogMonths 이 개월 수 이상이면 나이 든 강아지로 본다
 * @param olderDogMinCategoryOrder 나이 든 강아지에게 보여 줄 카테고리 정렬 값의 최솟값. prod 기준 3은 주니어 교육이다
 */
@Validated
@ConfigurationProperties("meongcoach.home.quick-topic")
public record QuickTopicProperties(@Positive int olderDogMonths, @Positive int olderDogMinCategoryOrder) {
}
