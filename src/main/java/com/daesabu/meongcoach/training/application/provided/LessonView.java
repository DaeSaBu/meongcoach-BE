package com.daesabu.meongcoach.training.application.provided;

/**
 * 레슨 조회 결과. 사용자의 반복 완료 횟수를 함께 담는다.
 */
public record LessonView(Long id, String title, int sortOrder, int estimatedMinutes, int completedCount) {
}
