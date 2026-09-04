package com.daesabu.meongcoach.training.application.provided;

import com.daesabu.meongcoach.training.domain.CurriculumStatus;

/**
 * 커리큘럼 조회 결과. 사용자의 레슨 완료 수로 계산한 진행 상태를 함께 담는다.
 */
public record CurriculumResult(Long id, String title, int totalLessons, int completedLessons,
		CurriculumStatus status) {
}
